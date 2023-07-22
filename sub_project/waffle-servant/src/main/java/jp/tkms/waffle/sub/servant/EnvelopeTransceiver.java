package jp.tkms.waffle.sub.servant;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EnvelopeTransceiver {
  static final byte B_DLE = 0x10;
  static final byte B_STX = 0x01;
  static final byte B_SOH = 0x02;
  static final byte B_ETX = 0x03;
  static final byte B_EOT = 0x04;
  static final byte B_ETB = 0x17;
  static final byte B_EM = 0x19;
  static final byte B_ESC = 0x1b;
  static final byte[] TAG_BEGIN = {B_DLE, B_STX};
  static final byte[] TAG_BEGIN_F = {B_DLE, B_SOH};
  static final byte[] TAG_END = {B_DLE, B_ETX};
  static final byte[] TAG_EXECUTE = {B_DLE, B_EOT};
  static final byte[] TAG_EXECUTE_F = {B_DLE, B_ETB};
  static final byte[] TAG_BYE = {B_DLE, B_EM};
  static final byte[] TAG_ESCAPE = {B_DLE, B_ESC};
  private static final long TIMEOUT = 15000;
  private static final long MAX_STREAM_SIZE = 1024 * 1024; // 1MB

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
      try {
        flush();
        try {
          inputStreamProcessor.join(TIMEOUT);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } catch (IOException e) {
        System.err.println("Stream is broken");
      }
    }
  }

  public void send(Envelope envelope) throws Exception {
    if (envelope.getFileSize() < MAX_STREAM_SIZE) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      envelope.save(stream);
      envelope.clear();
      synchronized (outputStream) {
        outputStream.write(TAG_BEGIN);
        sanitizeAndWrite(new ByteArrayInputStream(stream.toByteArray()), outputStream);
        outputStream.write(TAG_END);
        sanitizeAndWrite(new ByteArrayInputStream(toSHA1(stream.toByteArray())), outputStream);
        outputStream.write(TAG_EXECUTE);
        outputStream.write("\n\n".getBytes());
        outputStream.flush();
      }
    } else {
      Path tmpFile = Paths.get(".waffle-" + UUID.randomUUID());
      Files.createFile(tmpFile);
      tmpFile.toFile().deleteOnExit();
      envelope.save(tmpFile);
      envelope.clear();
      try (InputStream fileStream = new BufferedInputStream(new FileInputStream(tmpFile.toFile()))) {
        synchronized (outputStream) {
          outputStream.write(TAG_BEGIN_F);
          sanitizeAndWrite(fileStream, outputStream);
          outputStream.write(TAG_END);
          sanitizeAndWrite(new ByteArrayInputStream(String.valueOf(Files.size(tmpFile)).getBytes()), outputStream);
          outputStream.write(TAG_EXECUTE_F);
          outputStream.write("\n\n".getBytes());
          outputStream.flush();
        }
      }
      Files.deleteIfExists(tmpFile);
    }
  }

  private void sanitizeAndWrite(InputStream in, OutputStream out) {
    byte[] buf = new byte[1];
    try {
      while (in.read(buf, 0, 1) != -1) {
        if (buf[0] == B_DLE) {
          out.write(TAG_ESCAPE);
        } else {
          out.write(buf);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    private Consumer<InputStream> messageProcessor;
    private Runnable finalizer;
    private AtomicBoolean aliveFlag = new AtomicBoolean(true);
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream secondBuffer = null;
    private Path tmpFilePath = null;
    private OutputStream fileOutputStream = null;

    public InputStreamProcessor(InputStream inputStream, Consumer<InputStream> messageProcessor, Runnable finalizer) {
      this.inputStream = inputStream;
      this.messageProcessor = messageProcessor;
      this.finalizer = finalizer;
    }

    public void shutdown() {
      (new Thread(finalizer)).start();
      aliveFlag.set(false);
      clearFileStream();
    }

    @Override
    public void run() {
      boolean isFileMode = false;
      boolean isTagMode = false;
      byte[] buf = new byte[1];
      try {
        while (aliveFlag.get() && inputStream.read(buf, 0, 1) != -1) {
          if (buf[0] == B_DLE) {
            isTagMode = true;
          } else if (isTagMode) {
            isTagMode = false;
            switch (buf[0]) {
              case B_STX:
                isFileMode = false;
                resetBuffer();
                break;
              case B_SOH:
                isFileMode = true;
                startTmpFileStream();
                break;
              case B_ETX:
                if (isFileMode) {
                  closeMessageFile();
                  isFileMode = false;
                }
                pushBuffer();
                break;
              case B_EOT:
                processMessage();
                break;
              case B_ETB:
                processMessageFile();
                break;
              case B_ESC:
                if (isFileMode) {
                  fileOutputStream.write(B_DLE);
                } else  {
                  buffer.write(B_DLE);
                }
                break;
              case B_EM:
                shutdown();
                return;
              default:
                if (isFileMode) {
                  fileOutputStream.write(B_DLE);
                  fileOutputStream.write(buf);
                } else  {
                  buffer.write(B_DLE);
                  buffer.write(buf);
                }
            }
          } else if (isFileMode) {
            fileOutputStream.write(buf);
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
      } else {
        System.err.println("RECEIVED ENVELOPE IS BROKEN: " + buffer.size() + " bytes (" + secondBuffer.size() + " bytes)");
      }
    }

    private boolean checkMessageFileConsistency() {
      if (buffer != null && tmpFilePath != null && Files.exists(tmpFilePath)) {
        try {
          return String.valueOf(Files.size(tmpFilePath)).equals(new String(buffer.toByteArray()).trim());
        } catch (IOException e) {
          return false;
        }
      }
      return false;
    }

    private void processMessageFile() {
      if (checkMessageFileConsistency()) {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(tmpFilePath.toFile()))) {
          messageProcessor.accept(stream);
        } catch (Exception e) {
          System.err.println(e.getMessage() + " : " + tmpFilePath);
        }
        clearFileStream();
      } else {
        System.err.println("RECEIVED ENVELOPE IS BROKEN: " + tmpFilePath);
      }
    }

    private void closeMessageFile() {
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (IOException e) {
          //NOP
        } finally {
          fileOutputStream = null;
        }
      }
    }

    private void startTmpFileStream() {
      clearFileStream();
      tmpFilePath = Paths.get(".waffle-" + UUID.randomUUID());
      try {
        Files.createFile(tmpFilePath);
        tmpFilePath.toFile().deleteOnExit();
        fileOutputStream = new BufferedOutputStream(new FileOutputStream(tmpFilePath.toFile()));
      } catch (IOException e) {
        //NOP
      }
    }

    private void clearFileStream() {
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (IOException e) {
          //NOP
        } finally {
          fileOutputStream = null;
        }
      }
      if (tmpFilePath != null) {
        try {
          Files.deleteIfExists(tmpFilePath);
        } catch (IOException e) {
          //NOP
        }
        tmpFilePath = null;
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
