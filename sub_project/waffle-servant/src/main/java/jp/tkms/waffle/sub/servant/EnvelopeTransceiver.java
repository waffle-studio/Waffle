package jp.tkms.waffle.sub.servant;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnvelopeTransceiver {
  static final byte B_ESC = 0x1b;
  static final byte B_STX = 0x02;
  static final byte B_ETX = 0x03;
  static final byte B_EOT = 0x04;
  static final byte B_EM = 0x19;
  static final byte B_SE = 0x65;
  static final byte[] TAG_BEGIN = {B_ESC, B_STX};
  static final byte[] TAG_END = {B_ESC, B_ETX};
  static final byte[] TAG_EXECUTE = {B_ESC, B_EOT};
  static final byte[] TAG_BYE = {B_ESC, B_EM};
  static final byte[] TAG_ESCAPE = {B_ESC, B_SE};
  private static final long TIMEOUT = 10000;

  InputStreamProcessor inputStreamProcessor;
  BufferedOutputStream outputStream;
  BufferedInputStream inputStream;
  boolean isAlive = true;

  public EnvelopeTransceiver(Path baseDirectory, OutputStream outputStream, InputStream inputStream, BiConsumer<EnvelopeTransceiver, Envelope> messageProcessor) {
    this.outputStream = new BufferedOutputStream(outputStream);
    this.inputStream = new BufferedInputStream(inputStream);
    inputStreamProcessor = new InputStreamProcessor(this.inputStream, s -> {
      try {
        messageProcessor.accept(this, Envelope.loadAndExtract(baseDirectory, s));
      } catch (Exception e) {
        e.printStackTrace();
      }
    },
      () -> {
        try {
          shutdown();
          inputStream.close();
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    inputStreamProcessor.start();
  }

  public void flush() throws IOException {
    outputStream.flush();
  }

  public void shutdown() throws IOException {
    if (isAlive) {
      isAlive = false;
      outputStream.write(TAG_BYE);
      flush();
      try {
        inputStreamProcessor.join(TIMEOUT);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void send(Envelope envelope) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    envelope.save(stream);
    envelope.clear();

    outputStream.write(TAG_BEGIN);
    outputStream.write(sanitize(stream.toByteArray()));
    outputStream.write(TAG_END);
    outputStream.write(sanitize(toSHA1(stream.toByteArray())));
    outputStream.write(TAG_EXECUTE);
    outputStream.flush();
  }

  private byte[] sanitize(byte[] bytes) {
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    byte[] buf = new byte[1];
    try {
      while (stream.read(buf, 0, 1) != -1) {
        if (buf[0] == B_ESC) {
          resultStream.write(TAG_ESCAPE);
        } else {
          resultStream.write(buf);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return resultStream.toByteArray();
  }

  public void waitForShutdown() {
    while (isAlive) {
      try {
        inputStreamProcessor.join(TIMEOUT);
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  private static class InputStreamProcessor extends Thread {
    private InputStream inputStream;
    private Consumer<ByteArrayInputStream> messageProcessor;
    private Runnable finalizer;
    private AtomicBoolean aliveFlag = new AtomicBoolean(true);
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream secondBuffer = null;

    public InputStreamProcessor(InputStream inputStream, Consumer<ByteArrayInputStream> messageProcessor, Runnable finalizer) {
      this.inputStream = inputStream;
      this.messageProcessor = messageProcessor;
      this.finalizer = finalizer;
    }

    public void shutdown() {
      (new Thread(finalizer)).start();
      aliveFlag.set(false);
    }

    @Override
    public void run() {
      boolean isTagMode = false;
      byte[] buf = new byte[1];
      try {
        while (aliveFlag.get() && inputStream.read(buf, 0, 1) != -1) {
          if (buf[0] == B_ESC) {
            isTagMode = true;
          } else if (isTagMode) {
            isTagMode = false;
            switch (buf[0]) {
              case B_STX:
                resetBuffer();
                break;
              case B_ETX:
                pushBuffer();
                break;
              case B_EOT:
                processMessage();
                break;
              case B_SE:
                buffer.write(B_ESC);
                break;
              case B_EM:
                shutdown();
                return;
              default:
                buffer.write(B_ESC);
                buffer.write(buf);
            }
          } else {
            buffer.write(buf);
          }
        }
      } catch (IOException e) {
        shutdown();
      }
    }

    private boolean checkConsistency() {
      if (buffer != null && secondBuffer != null) {
        return Arrays.equals(buffer.toByteArray(), toSHA1(secondBuffer.toByteArray()));
      }
      return false;
    }

    private void resetBuffer() {
      buffer = new ByteArrayOutputStream();
      secondBuffer = null;
    }

    private void pushBuffer() {
      secondBuffer = buffer;
      buffer = new ByteArrayOutputStream();
    }

    private void processMessage() {
      if (checkConsistency()) {
        messageProcessor.accept(new ByteArrayInputStream(secondBuffer.toByteArray()));
      }
    }
  }

  public static byte[] toSHA1(byte[] bytes) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    messageDigest.update(bytes);
    return messageDigest.digest();
  }
}
