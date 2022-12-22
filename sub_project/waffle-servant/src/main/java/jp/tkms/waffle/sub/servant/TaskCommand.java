package jp.tkms.waffle.sub.servant;

import java.nio.file.Path;

public abstract class TaskCommand {
  Path baseDirectory;
  Path taskJsonPath;
  Path taskDirectory;

  public TaskCommand(Path baseDirectory, Path taskJsonPath) throws Exception {
    this.baseDirectory = baseDirectory;

    if (!taskJsonPath.isAbsolute()) {
      taskJsonPath = baseDirectory.resolve(taskJsonPath);
    }

    this.taskJsonPath = taskJsonPath.normalize();
    this.taskDirectory = taskJsonPath.getParent().normalize();
  }

  public Path getTaskDirectory() {
    return taskDirectory;
  }
}
