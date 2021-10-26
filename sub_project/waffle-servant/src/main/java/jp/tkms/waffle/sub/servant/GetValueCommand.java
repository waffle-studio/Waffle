package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class GetValueCommand extends TaskCommand {
  public static final String GET = ".GET";
  public static final String RESPONSE = ".res";

  String key;
  String operator;
  String value;
  int timeout;

  public GetValueCommand(Path baseDirectory, Path taskJsonPath, String key, String operator, String value, int timeout) throws Exception {
    super(baseDirectory, taskJsonPath);
    this.key = key;
    this.operator = operator;
    this.value = value;
    this.timeout = timeout;
  }

  public GetValueCommand(Path baseDirectory, Path taskJsonPath, String key, String operator, String value) throws Exception {
    this(baseDirectory, taskJsonPath, key, operator, value, -1);
  }

  public GetValueCommand(Path baseDirectory, Path taskJsonPath, String key) throws Exception {
    this(baseDirectory, taskJsonPath, key, "", "", -1);
  }

  public void run() {
    try {
      Path directory = taskDirectory.resolve(GET).normalize();
      Files.createDirectories(directory);
      UUID uuid = UUID.randomUUID();

      Path requestFilePath = directory.resolve(uuid.toString());
      Path responseFilePath = directory.resolve(uuid.toString());

      Files.writeString(requestFilePath, key + "\n" + operator + "\n" + value);

      WatchService watchService = FileSystems.getDefault().newWatchService();
      directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);

      for (int count = 0; count > 0 || count == -1; count += 1) {
        TimeUnit.SECONDS.sleep(1);
        if (Files.exists(responseFilePath)) {
          String value = new String(Files.readAllBytes(responseFilePath));
          if (value.endsWith("\n")) {
            System.out.print(value);
            break;
          }
        }
      }

      Files.delete(requestFilePath);
      Files.deleteIfExists(responseFilePath);
    } catch (IOException | InterruptedException e) {
      //NOP
    }
  }
}
