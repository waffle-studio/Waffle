package jp.tkms.waffle.sub.vje;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractExecutor {
  public static final Path JOBS_PATH = Paths.get("jobs");

  public AbstractExecutor() {
    try {
      Files.createDirectories(JOBS_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
