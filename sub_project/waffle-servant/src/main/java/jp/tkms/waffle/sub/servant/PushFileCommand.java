package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PushFileCommand extends TaskCommand {
  public static final String PUSH = ".PUSH";

  Path path;

  public PushFileCommand(Path baseDirectory, Path taskJsonPath, Path path) throws Exception {
    super(baseDirectory, taskJsonPath);
    this.path = path;
  }

  public void run() {
    try {
      Path pushDirectory = taskDirectory.resolve(PUSH).normalize();
      Path executingBaseDirectory = taskDirectory.resolve(Constants.BASE).normalize();
      Path sourcePath = executingBaseDirectory.resolve(path);
      Path destinationPath = pushDirectory.resolve(path).normalize();

      if (!Files.exists(sourcePath) || !destinationPath.startsWith(pushDirectory) || !Files.isDirectory(sourcePath)) {
        System.err.println("invalid path: " + path);
      }

      Files.createDirectories(destinationPath.getParent());
      Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      //NOP
    }
  }
}
