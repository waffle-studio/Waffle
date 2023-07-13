package jp.tkms.waffle.sub.servant;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class EventReader {
  private static final String CURSOR_FILE_SUFFIX = ".cursor";

  private Path recordPath;
  private Path cursorPath;

  public EventReader(Path baseDirectory, Path workingDirectory) {
    if (!workingDirectory.isAbsolute()) {
      workingDirectory = baseDirectory.resolve(workingDirectory);
    }
    this.recordPath = workingDirectory.resolve(Constants.EVENT_FILE);
    this.cursorPath = this.recordPath.getParent().resolve(this.recordPath.getFileName().toString() + CURSOR_FILE_SUFFIX);
  }

  public void process(BiConsumer<String, String> consumer) {
    long fileSize = 0;
    try {
      fileSize = Files.size(recordPath);
    } catch (IOException e) {
      return;
    }

    Path singleValueDirPath = recordPath.getParent().resolve(recordPath.getFileName().toString() + ".d");
    if (Files.isDirectory(singleValueDirPath)) {
      Path ignoreFlagPath = singleValueDirPath.getParent().resolve(singleValueDirPath.getFileName().toString() + DirectoryHash.IGNORE_FLAG);
      try {
        if (!Files.exists(ignoreFlagPath)) {
          Files.createFile(ignoreFlagPath);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }


    long cursor = 0;
    if (Files.exists(cursorPath)) {
      try {
        cursor = Long.parseLong(new String(Files.readAllBytes(cursorPath)));
      } catch (Exception | Error e) {
        e.printStackTrace();
      }
    }

    try {
      if (Files.size(recordPath) > cursor) {
        try (FileInputStream inputStream = new FileInputStream(recordPath.toFile());) {
          inputStream.skip(cursor);
          try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            char[] buf = new char[8];
            int len = 0;
            StringBuilder stringBuilder = new StringBuilder();
            long localCursor = 0;
            while (reader.read(buf, 0, 1) != -1) {
              len = getLengthOf(buf);
              localCursor += len;

              if (isSameChars(buf, Constants.EVENT_SEPARATOR.charAt(0))) {
                cursor += localCursor;
                String line = stringBuilder.toString();

                int index = line.indexOf(Constants.EVENT_VALUE_SEPARATOR);
                if (index >= 0 && index +1 < line.length()) {
                  consumer.accept(line.substring(0, index), line.substring(index +1));
                }

                localCursor = 0;
                stringBuilder = new StringBuilder();
              } else {
                stringBuilder.append(buf, 0, len);
              }
            }
          }
        }
      }

      FileWriter writer = new FileWriter(cursorPath.toFile(), StandardCharsets.UTF_8, false);
      writer.write(String.valueOf(cursor));
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private int searchingCursor = 0;
  private int getLengthOf(char[] ch) {
    for (searchingCursor = 0; searchingCursor < ch.length && ch[searchingCursor] != '\0'; searchingCursor += 1) {
      // NOP
    }
    return searchingCursor;
  }

  private boolean isSameChars(char[] ch, char c) {
    return ch[0] == c && ch[1] == '\0';
  }
}
