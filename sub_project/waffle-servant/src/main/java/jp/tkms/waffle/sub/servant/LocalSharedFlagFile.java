package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class LocalSharedFlagFile {
  public static final String FLAG_PREFIX = ".WAFFLE_LOCAL_SHARED";
  public enum Level { None, Run, Workspace, Project };
  private static LocalSharedFlagFile noneFlag = new LocalSharedFlagFile();

  private Level level;

  private LocalSharedFlagFile() {
    level = Level.None;
  }

  private LocalSharedFlagFile(Path path) throws IOException {
    if (Files.readString(path).toLowerCase(Locale.ROOT).startsWith("w")) {
      level = Level.Workspace;
    } else if (Files.readString(path).toLowerCase(Locale.ROOT).startsWith("r")) {
      level = Level.Run;
    } else {
      level = Level.Project;
    }
  }

  public Level getLevel() {
    return level;
  }

  public static LocalSharedFlagFile toFlag(Path path) {
    if (Files.isRegularFile(path)) {
      try {
        return new LocalSharedFlagFile(path);
      } catch (IOException e) {
        return noneFlag;
      }
    }
    return noneFlag;
  }

  public static String getFlagFileName(String fileName) {
    return FLAG_PREFIX + "." + fileName;
  }

  public static String getFlagFileName() {
    return FLAG_PREFIX;
  }
}
