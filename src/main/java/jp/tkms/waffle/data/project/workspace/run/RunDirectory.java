package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunDirectory implements DataDirectory {
  private Path path;

  public RunDirectory(Path path) {
    if (path.isAbsolute()) {
      this.path = path;
    } else {
      this.path = Constants.WORK_DIR.resolve(path);
    }
  }

  public RunDirectory(String path) {
    this(Paths.get(path));
  }

  @Override
  public Path getPath() {
    return path;
  }
}
