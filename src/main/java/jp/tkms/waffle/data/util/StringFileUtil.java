package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StringFileUtil {
  public static void write(Path path, String contents, boolean isAppend) {
    try {
      Path directoryPath = path.getParent();
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }

      synchronized (PathLocker.getLocker(path)) {
        FileWriter fileWriter = new FileWriter(path.toFile(), isAppend);
        fileWriter.write(contents);
        fileWriter.close();
      }
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public static void append(Path path, String contents) {
    write(path, contents, true);
  }

  public static void write(Path path, String contents) {
    write(path, contents, false);
  }

  public static String read(Path path) {
    String contents;
    try {
      synchronized (PathLocker.getLocker(path)) {
        contents = new String(Files.readAllBytes(path));
      }
    } catch (IOException e) {
      contents = "";
    }
    return contents;
  }
}
