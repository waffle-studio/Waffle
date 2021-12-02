package jp.tkms.waffle.data.util;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

public class ChildElementsArrayList<T> extends ArrayList<T> {
  public static final String DOT_SORT = ".SORT";

  public enum Mode {
    All, OnlyNormal, OnlyHidden, OnlyFavorite
  }

  public ChildElementsArrayList getList(Path baseDirectory, Mode mode, Function<String, T> getInstance) {
    if (Files.exists(baseDirectory)) {
      try {
        if (Mode.All.equals(mode)) {
          try (Stream<Path> paths = Files.list(baseDirectory)) {
            paths.filter(path -> Files.isDirectory(path)).sorted(
              Comparator.comparingLong(path -> {
                try {
                  return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
                } catch (IOException e) {
                  return 0;
                }
              })
            ).forEach(path -> {
              add(getInstance.apply(path.getFileName().toString()));
            });
          }
        /*
        for (File file : baseDirectory.toFile().listFiles()) {
          if (file.isDirectory()) {
            add(getInstance.apply(file.getName()));
          }
        }
         */
        } else {
          try (Stream<Path> paths = Files.list(baseDirectory)) {
            paths.filter(path -> Files.isDirectory(path)).sorted(
              Comparator.comparingLong(path -> {
                try {
                  Path sortFilePath = path.resolve(DOT_SORT);
                  if (!Files.exists(sortFilePath)) {
                    sortFilePath = path;
                  }
                  return Files.readAttributes(sortFilePath, BasicFileAttributes.class).creationTime().toInstant().toEpochMilli() * -1;
                } catch (IOException e) {
                  return 0;
                }
              })
            ).forEach(path -> {
              String name = path.getFileName().toString();
              if (
                (Mode.OnlyNormal.equals(mode) && !name.startsWith("."))
                  || (Mode.OnlyHidden.equals(mode) && name.startsWith("."))
                  || (Mode.OnlyFavorite.equals(mode) && Files.exists(path.resolve(Constants.DOT_FAVORITE)))
              ) {
                add(getInstance.apply(name));
              }
            });
          }
          /*
          for (File file : baseDirectory.toFile().listFiles()) {
            if (file.isDirectory() && (
              (Mode.OnlyNormal.equals(mode) && !file.getName().startsWith("."))
                ||
                (Mode.OnlyHidden.equals(mode) && file.getName().startsWith("."))
                ||
                (Mode.OnlyFavorite.equals(mode) && Files.exists(file.toPath().resolve(Constants.DOT_FAVORITE)))
            ) ) {
              add(getInstance.apply(file.getName()));
            }
          }
           */
        }
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
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

  public ChildElementsArrayList getList(Path baseDirectory, Function<String, T> getInstance) {
    return getList(baseDirectory, Mode.All, getInstance);
  }
}
