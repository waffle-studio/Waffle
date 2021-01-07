package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class FileBuffer {
  private static ConcurrentHashMap<Path, Object> map = new ConcurrentHashMap<>();

  public static void write(Path path, String contents) {
    Object locker = null;
    path = path.toAbsolutePath();

    synchronized (map) {
      locker = map.get(path);
      if (locker == null) {
        locker = new Object();
        map.put(path, locker);
      }
    }

    synchronized (locker) {
      try {
        Path directoryPath = path.getParent();
        if (!Files.exists(directoryPath)) {
          Files.createDirectories(directoryPath);
        }

        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(contents);
        filewriter.close();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }

      map.remove(path);
    }
  }

  public static String read(Path path) {
    Object locker = null;
    path = path.toAbsolutePath();

    synchronized (map) {
      locker = map.get(path);
      if (locker == null) {
        locker = new Object();
        map.put(path, locker);
      }
    }

    String contents;
    synchronized (locker) {
      try {
        contents = new String(Files.readAllBytes(path));
      } catch (IOException e) {
        contents = "";
      }
      map.remove(path);
    }
    return contents;
  }
}
