package jp.tkms.waffle.sub.servant;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class OutputProcessor extends Thread {
  private InputStream inputStream;
  private Path outputFilePath;
  private EventRecorder eventRecorder;

  public OutputProcessor(InputStream inputStream, Path outputFilePath, EventRecorder eventRecorder) {
    this.inputStream = inputStream;
    this.outputFilePath = outputFilePath;
    this.eventRecorder = eventRecorder;
  }

  @Override
  public void run() {
    char[] buf = new char[8];
    int len = 0;

    try (FileWriter writer = new FileWriter(outputFilePath.toFile(), StandardCharsets.UTF_8, false)) {
      try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
        while (reader.read(buf, 0, 1) != -1) {
          //System.out.print(buf);

          len = getLengthOf(buf);
          writer.write(buf, 0, len);

          if (len == 1 && buf[0] == '\n') {
            writer.flush();
          }

          if (eventRecorder != null) {
            eventRecorder.input(buf, len);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return;
  }

  private int searchingCursor = 0;
  private int getLengthOf(char[] ch) {
    for (searchingCursor = 0; searchingCursor < ch.length && ch[searchingCursor] != '\0'; searchingCursor += 1) {
      // NOP
    }
    return searchingCursor;
  }
}
