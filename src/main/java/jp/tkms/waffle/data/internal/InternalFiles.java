package jp.tkms.waffle.data.internal;

import jp.tkms.waffle.Constants;

import java.nio.file.Path;

public class InternalFiles {
  public static Path getPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL);
  }

  public static Path getPath(Path path) {
    return getPath().resolve(path);
  }

  public static Path getPath(String path) {
    return getPath().resolve(path);
  }

  public static Path getLocalPath() {
    return Constants.WORK_DIR.relativize(getPath());
  }

  public static Path getLocalPath(Path path) {
    return Constants.WORK_DIR.relativize(getPath(path));
  }

  public static Path getLocalPath(String path) {
    return Constants.WORK_DIR.relativize(getPath(path));
  }
}
