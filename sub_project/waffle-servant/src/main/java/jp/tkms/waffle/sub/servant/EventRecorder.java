package jp.tkms.waffle.sub.servant;

import javax.swing.plaf.IconUIResource;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class EventRecorder {
  private static String EVENT_LABEL = "[WAFFLE_RESULT:";

  private Path recordPath;

  public EventRecorder(Path baseDirectory, Path recordPath) {
    if (recordPath.isAbsolute()) {
      this.recordPath = recordPath;
    } else {
      this.recordPath = baseDirectory.resolve(recordPath);
    }
  }

  private void write(String name, String value) throws IOException {
    FileWriter writer = new FileWriter(recordPath.toFile(), StandardCharsets.UTF_8, true);
    writer.write(name + "\t" + value + "\n");
    writer.close();
  }

  public void record() {
    StringBuilder nameBuilder = null;
    StringBuilder valueBuilder = null;
    int cursor = 0;
    boolean escape = false;
    char[] buffer = new char[64];

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      System.gc();

      while (reader.read(buffer, 0, 1) != -1) {
        String s = new String(buffer);
        s = s.substring(0, s.indexOf('\0'));
        System.out.print(s);

        if (valueBuilder != null) {
          if (!escape && s.equals("]")) {
            write(nameBuilder.toString(), valueBuilder.toString());
            nameBuilder = null;
            valueBuilder = null;
          } else if (!escape && s.equals("\\")) {
            escape = true;
          } else {
            escape = false;
            valueBuilder.append(s);
          }
        } else if (nameBuilder != null) {
          if (!escape && s.equals(":")) {
            valueBuilder = new StringBuilder();
          } else if (!escape && s.equals("\\")) {
            escape = true;
          } else {
            escape = false;
            nameBuilder.append(s);
          }
        } else if (s.equals(EVENT_LABEL.substring(cursor, cursor +1))) {
          cursor += 1;
          if (EVENT_LABEL.length() == cursor) {
            cursor = 0;
            nameBuilder = new StringBuilder();
          }
        } else if (s.equals("[")) {
          cursor = 1;
        } else {
          cursor = 0;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
