package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class LocalSharedFlag {
  public static final String FLAG_PREFIX = ".WAFFLE_LOCAL_SHARED";
  public enum Level { None, Run, Workspace, Project };
  private static LocalSharedFlag noneFlag = new LocalSharedFlag();

  private Level level;

  private LocalSharedFlag() {
    level = Level.None;
  }

  private LocalSharedFlag(Path path) throws IOException {
    if (Files.readString(path).toLowerCase(Locale.ROOT).startsWith("w")) {
      level = Level.Workspace;
    } else if (Files.readString(path).toLowerCase(Locale.ROOT).startsWith("p")) {
      level = Level.Project;
    } else {
      level = Level.Run;
    }
  }

  public Level getLevel() {
    return level;
  }

  public static LocalSharedFlag getFlag(Path path) {
    Path flagPath = null;

    if (Files.isDirectory(path)) {
      flagPath = path.getParent().resolve(getFlagFileName(path.getFileName().toString()));
      if (!Files.isRegularFile(flagPath)) {
        flagPath = path.resolve(getFlagFileName());
      }
    } else {
      flagPath = path.getParent().resolve(getFlagFileName(path.getFileName().toString()));
    }

    if (Files.isRegularFile(flagPath)) {
      try {
        return new LocalSharedFlag(flagPath);
      } catch (IOException e) {
        return noneFlag;
      }
    }
    return noneFlag;
  }

  public static String getFlagFileName(String fileName) {
    return FLAG_PREFIX + '.' + fileName;
  }

  public static String getFlagFileName() {
    return FLAG_PREFIX;
  }
}
