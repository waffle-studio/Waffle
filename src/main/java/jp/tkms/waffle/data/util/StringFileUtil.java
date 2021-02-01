package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StringFileUtil {
  public static void write(Path path, String contents) {
    try {
      Path directoryPath = path.getParent();
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }

      PathSemaphore pathSemaphore = PathSemaphore.acquire(path);
      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write(contents);
      filewriter.close();
      pathSemaphore.release();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public static String read(Path path) {
    String contents;
    try {
      PathSemaphore pathSemaphore = PathSemaphore.acquire(path);
      contents = new String(Files.readAllBytes(path));
      pathSemaphore.release();
    } catch (IOException e) {
      contents = "";
    }
    return contents;
  }
}
