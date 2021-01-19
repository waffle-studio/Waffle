package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Function;

public class ChildElementsArrayList<T> extends ArrayList<T> {
  public ChildElementsArrayList getList(Path baseDirectory, Function<String, T> getInstance) {
    if (Files.exists(baseDirectory)) {
      for (File file : baseDirectory.toFile().listFiles()) {
        if (file.isDirectory()) {
          add(getInstance.apply(file.getName()));
        }
      }
    } else {
      try {
        Files.createDirectories(baseDirectory);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    return this;
  }
}
