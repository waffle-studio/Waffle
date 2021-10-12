package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.data.DataDirectory;

import java.nio.file.Path;

public class RunDirectory implements DataDirectory {
  private Path path;

  public RunDirectory(Path path) {
    this.path = path;
  }

  @Override
  public Path getPath() {
    return path;
  }
}
