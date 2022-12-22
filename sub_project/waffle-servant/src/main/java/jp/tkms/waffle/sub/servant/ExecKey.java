package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class ExecKey {
  UUID uuid;

  public ExecKey(UUID key) {
    this.uuid = key;
  }

  public ExecKey(String key) {
    this(UUID.fromString(key));
  }

  public ExecKey(Path taskDirectory) {
    Path path = getKeyPath(taskDirectory);
    try {
      if (Files.exists(path)) {
        this.uuid = UUID.fromString(new String(Files.readAllBytes(path)));
      } else {
        this.uuid = UUID.randomUUID();
      }
    } catch (IOException e) {
      this.uuid = UUID.randomUUID();
    }
  }

  public ExecKey() {
    this(UUID.randomUUID());
  }

  Path getKeyPath(Path taskDirectory) {
    return taskDirectory.resolve(Constants.EXEC_KEY);
  }

  public void save(Path taskDirectory) {
    try {
      Files.writeString(getKeyPath(taskDirectory), uuid.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean authorize(Path taskDirectory) {
    return equals(new ExecKey(taskDirectory));
  }

  @Override
  public String toString() {
    return uuid.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }
}
