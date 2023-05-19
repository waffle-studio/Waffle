package jp.tkms.waffle.sub.servant;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;
import jp.tkms.waffle.sub.servant.message.request.PutValueMessage;
import jp.tkms.waffle.sub.servant.message.response.SendValueMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class GetValueCommand extends TaskCommand {
  public static final String GET = ".GET";
  public static final String REQUEST = ".req";
  public static final String RESPONSE = ".res";
  public static final char RECORD_SEPARATING_MARK = '\n';

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

  public boolean run() {
    boolean isSuccess = false;

    try {
      Path directory = taskDirectory.resolve(GET).normalize();
      Files.createDirectories(directory);
      UUID uuid = UUID.randomUUID();

      Path requestFilePath = directory.resolve(uuid.toString() + REQUEST);
      Path responseFilePath = directory.resolve(uuid.toString() + RESPONSE);

      Files.writeString(requestFilePath, key + RECORD_SEPARATING_MARK + operator + RECORD_SEPARATING_MARK + value);

      for (int count = 0; count > 0 || timeout == -1; count += 1) {
        TimeUnit.SECONDS.sleep(1);
        if (Files.exists(responseFilePath)) {
          String value = new String(Files.readAllBytes(responseFilePath));
          if (value.endsWith(String.valueOf(RECORD_SEPARATING_MARK))) {
            System.err.print(value);
            isSuccess = true;
            break;
          }
        }
      }

      Files.delete(requestFilePath);
      Files.deleteIfExists(responseFilePath);
    } catch (IOException | InterruptedException e) {
      //NOP
    }

    return isSuccess;
  }

  public static void process(Path workingDirectory, Consumer<SendValueMessage> consumer) {
    Path directory = workingDirectory.resolve(GET).normalize();
    if (Files.exists(directory) && Files.isDirectory(directory)) {
      try (Stream<Path> files = Files.list(directory)) {
        files.forEach(p -> {
          if (p.getFileName().toString().endsWith(REQUEST)) {
            try {
              String uuid = p.getFileName().toString();
              uuid = uuid.substring(0, uuid.length() - REQUEST.length());
              String[] record = (new String(Files.readAllBytes(p))).split(String.valueOf(RECORD_SEPARATING_MARK), 3);
              consumer.accept(new SendValueMessage(
                workingDirectory, uuid, record[0], record[1], record[2]
              ));
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void putResponse(PutValueMessage message) {
    Path responseFilePath = message.getWorkingDirectory().resolve(GET).resolve(message.getId() + RESPONSE);
    try {
      Files.createDirectories(responseFilePath.getParent());
      Files.writeString(responseFilePath, message.getValue(), Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

