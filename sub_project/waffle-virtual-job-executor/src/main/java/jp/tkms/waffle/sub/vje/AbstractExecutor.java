package jp.tkms.waffle.sub.vje;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;

public abstract class AbstractExecutor {
  protected static final String BATCH_FILE = "batch.sh";
  public static final Path JOBS_PATH = Paths.get("jobs");

  private WatchService fileWatchService = null;
  private HashSet<String> runningJobList = new HashSet<>();
  private Object objectLocker = new Object();
  private int waitTime = 0;
  private int hesitationTime = 0;

  public AbstractExecutor(int waitTime, int hesitationTime) throws IOException {
    this.waitTime = waitTime;
    this.hesitationTime = hesitationTime;
    Files.createDirectories(JOBS_PATH);
    fileWatchService = FileSystems.getDefault().newWatchService();
    JOBS_PATH.register(fileWatchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
    checkJobs();
  }

  public void startPolling() {
    Thread waitTimer = null;
    try {
      WatchKey watchKey = null;
      while ((watchKey = fileWatchService.take()) != null) {
        if (waitTimer != null) {
          waitTimer.interrupt();
          waitTimer = null;
        }
        /*
        for (WatchEvent<?> event : watchKey.pollEvents()) {
          Path path = (Path)watchKey.watchable();
          System.out.println(path.toString());
        }
         */
        checkJobs();
        watchKey.reset();

        if (runningJobList.isEmpty()) {
          waitTimer = new Thread() {
            @Override
            public void run() {
              try {
                sleep(waitTime);
                shutdown();
              } catch (InterruptedException e) {
              }
            }
          };
          waitTimer.start();
        }
      }
    } catch (InterruptedException e) {
      if (waitTimer != null) {
        waitTimer.interrupt();
      }
      return;
    }
  }

  public void shutdown() {
    synchronized (objectLocker) {
      try {
        fileWatchService.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    checkJobs();

    for (int countDown = hesitationTime; countDown >= 0 && !runningJobList.isEmpty(); countDown--) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void checkJobs() {
    synchronized (objectLocker) {
      for (String jobName : runningJobList) {
        if (!Files.exists(JOBS_PATH.resolve(jobName))) {
          jobRemoved(jobName);
          runningJobList.remove(jobName);
        }
      }
      for (File file : JOBS_PATH.toFile().listFiles()) {
        if (!runningJobList.contains(file.getName())) {
          jobAdded(file.getName());
          runningJobList.add(file.getName());
        }
      }
    }
  }

  protected void jobAdded(String jobName) {
    System.out.println(jobName);
  }

  protected void jobRemoved(String jobName) {
    System.out.println(jobName);
  }

  protected void jobFinished(String jobName) {
    synchronized (objectLocker) {
      runningJobList.remove(jobName);
    }
  }

  protected Thread startJobThread(String jobName) {
    Thread thread = new Thread() {
      boolean isCanceled = false;
      Process process = null;

      @Override
      public void run() {
        if (isCanceled) {
          return;
        }

        try {
          process = new ProcessBuilder().directory(Paths.get("..").resolve("..").resolve(jobName).toFile())
            .command(BATCH_FILE).start();
          process.waitFor();
        } catch (Exception e) {
          e.printStackTrace();
        }

        jobFinished(jobName);
      }

      @Override
      public void interrupt() {
        isCanceled = true;
        if (process != null) {
          process.destroyForcibly();
        }
      }
    };

    thread.start();

    return thread;
  }
}
