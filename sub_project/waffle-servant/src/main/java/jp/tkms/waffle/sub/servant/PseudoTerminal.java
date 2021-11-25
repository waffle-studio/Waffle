package jp.tkms.waffle.sub.servant;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.DOMConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PseudoTerminal {
  public static final String TERMINAL = "TERMINAL";
  public static final String IN = "IN";
  public static final String OUT = "OUT";
  private Path directory;
  private Path inputFilePath;
  private Path outputFilePath;

  public PseudoTerminal(Path baseDirectory, String id) throws IOException {
    directory = baseDirectory.resolve(TERMINAL).resolve(id);
    inputFilePath = directory.resolve(IN);
    outputFilePath = directory.resolve(OUT);

    Files.createDirectories(directory);

    if (!Files.exists(inputFilePath)) {
      Files.createFile(inputFilePath);
    }

    if (!Files.exists(outputFilePath)) {
      Files.createFile(outputFilePath);
    }
  }

  public void run() throws InterruptedException, IOException {
    //PtyProcess pty = PtyProcess.exec(new String[]{System.getenv().getOrDefault("SHELL", "/bin/sh")}, System.getenv());
    PtyProcessBuilder processBuilder = new PtyProcessBuilder().
      setCommand(new String[]{System.getenv().getOrDefault("SHELL", "/bin/sh")}).
      setEnvironment(System.getenv()).
      setRedirectErrorStream(true);
    PtyProcess pty = processBuilder.start();

    InputFileReader inputFileReader = new InputFileReader(pty.getOutputStream());
    OutputFileWriter outputOutputFileWriter = new OutputFileWriter(pty.getInputStream());
    inputFileReader.start();
    outputOutputFileWriter.start();

    pty.waitFor();

    inputFileReader.interrupt();
    outputOutputFileWriter.join();

    Files.delete(inputFilePath);
  }

    public void shutdown() {
    }

    private class InputFileReader extends Thread {
    long cursor;
    OutputStream outputStream;

    InputFileReader(OutputStream outputStream) {
      this.outputStream = outputStream;
      cursor = 0;
    }

    @Override
    public void run() {
      try {
        while (true) {
          if (Files.size(inputFilePath) > cursor) {
            try (FileInputStream inputStream = new FileInputStream(inputFilePath.toFile())) {
              inputStream.skip(cursor);
              cursor += IOUtils.copy(inputStream, outputStream);
            }
          }
          TimeUnit.MILLISECONDS.sleep(500);
        }
      } catch (Exception e) {
        //NOP
      }
    }
  }

  private class OutputFileWriter extends Thread {
    InputStream inputStream;

    OutputFileWriter(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public void run() {
      int buf;
      try {
        while ((buf = inputStream.read()) != -1) {
          try (FileOutputStream outputStream = new FileOutputStream(outputFilePath.toFile(), true)) {
            do {
              outputStream.write(buf);
              buf = inputStream.read();
              if (buf == -1) {
                return;
              }
            } while (buf != '\n');
            outputStream.write(buf);
            outputStream.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
