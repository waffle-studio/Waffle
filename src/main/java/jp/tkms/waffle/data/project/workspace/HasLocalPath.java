package jp.tkms.waffle.data.project.workspace;

import jp.tkms.waffle.Constants;

import java.nio.file.Path;

public interface HasLocalPath {
  Path getPath();

  default Path getLocalPath() {
    Path path = getPath();
    if (path.isAbsolute()) {
      return toLocalPath(getPath());
    }
    return path;
  }

  static Path toLocalPath(Path path) {
    return Constants.WORK_DIR.relativize(path);
  }
}
