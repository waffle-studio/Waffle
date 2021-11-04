package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.message.response.PutFileMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
      Path sourcePath = path.toAbsolutePath();
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

  public static void process(Path workingDirectory, Consumer<PutFileMessage> consumer) {
    Path pushDirectory = workingDirectory.resolve(PUSH).toAbsolutePath().normalize();
    Path executingBaseDirectory = workingDirectory.resolve(Constants.BASE).toAbsolutePath().normalize();
    if (Files.exists(pushDirectory) && Files.isDirectory(pushDirectory)) {
      try (Stream<Path> files = Files.list(pushDirectory)) {
        files.forEach(p -> {
          try {
            consumer.accept(new PutFileMessage(executingBaseDirectory.resolve(pushDirectory.relativize(p)), p));
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
