package jp.tkms.waffle.sub.vje;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;

public abstract class AbstractExecutor {
  public static final Path JOBS_PATH = Paths.get("jobs");

  private WatchService fileWatchService = null;
  private HashSet<String> runningList = new HashSet<>();

  public AbstractExecutor() throws IOException {
    Files.createDirectories(JOBS_PATH);
    fileWatchService = FileSystems.getDefault().newWatchService();
    JOBS_PATH.register(fileWatchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
    checkJobs();
  }

  public void startPolling() {
    try {
      WatchKey watchKey = null;
      while ((watchKey = fileWatchService.take()) != null) {
        /*
        for (WatchEvent<?> event : watchKey.pollEvents()) {
          Path path = (Path)watchKey.watchable();
          System.out.println(path.toString());
        }
         */
        checkJobs();
        watchKey.reset();
      }
    } catch (InterruptedException e) {
      return;
    }
  }

  private void checkJobs() {
    for (File file : JOBS_PATH.toFile().listFiles()) {
      if (!runningList.contains(file.getName())) {
        System.out.println(file.getName());
        runningList.add(file.getName());
      }
    }
  }
}
