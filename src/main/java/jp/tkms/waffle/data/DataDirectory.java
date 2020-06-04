package jp.tkms.waffle.data;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface DataDirectory {
  Path getDirectoryPath();

  default void createNewFile(String fileName) {
    Path path = getDirectoryPath().resolve(fileName);
    try {
      Files.createDirectories(getDirectoryPath());

      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write("");
      filewriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  default String getFileContents(String fileName) {
    String contents = "";
    try {
      contents = new String(Files.readAllBytes(getDirectoryPath().resolve(fileName)));
    } catch (IOException e) {
    }
    return contents;
  }

  default void updateFileContents(String fileName, String contents) {
    Path path = getDirectoryPath().resolve(fileName);
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
}
