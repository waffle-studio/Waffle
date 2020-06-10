package jp.tkms.waffle.data;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface DataDirectory {
  Path getDirectoryPath();

  default void createNewFile(Path path) {
    if (! path.isAbsolute()) {
      path = getDirectoryPath().resolve(path);
    }
    try {
      Files.createDirectories(getDirectoryPath());
      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write("");
      filewriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  default String getFileContents(Path path) {
    if (! path.isAbsolute()) {
      path = getDirectoryPath().resolve(path);
    }
    String contents = "";
    try {
      contents = new String(Files.readAllBytes(path));
    } catch (IOException e) {
    }
    return contents;
  }

  default void updateFileContents(Path path, String contents) {
    if (! path.isAbsolute()) {
      path = getDirectoryPath().resolve(path);
    }
    if (Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(contents);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  default void createNewFile(String fileName) {
    createNewFile(Paths.get(fileName));
  }

  default String getFileContents(String fileName) {
    return getFileContents(Paths.get(fileName));
  }

  default void updateFileContents(String fileName, String contents) {
    updateFileContents(Paths.get(fileName), contents);
  }
}
