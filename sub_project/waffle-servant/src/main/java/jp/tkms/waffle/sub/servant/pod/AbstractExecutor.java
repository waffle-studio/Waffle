package jp.tkms.waffle.sub.servant.pod;

import jp.tkms.waffle.sub.servant.DirectoryHash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public abstract class AbstractExecutor {
  protected static final String BATCH_FILE = "./batch.sh";
  public static final Path BASE_PATH = Paths.get("BASE");
  public static final Path HASH_IGNORE_FILE_PATH = BASE_PATH.resolve(DirectoryHash.IGNORE_FLAG);
  public static final Path JOBS_PATH = BASE_PATH.resolve("JOBS");
  public static final Path ENTITIES_PATH = BASE_PATH.resolve("ENTITIES");
  public static final Path ALIVE_NOTIFIER_PATH = BASE_PATH.resolve("ALIVE_NOTIFIER");
  public static final Path LOCKOUT_FILE_PATH = ALIVE_NOTIFIER_PATH.resolve("LOCKOUT");

  //private WatchService fileWatchService = null;
  private Thread shutdownTimer;
  private boolean isAlive;
  private HashSet<String> runningJobList = new HashSet<>();
  private Object objectLocker = new Object();
  private int timeout = 0;
  private int waitingTimeCounter = 0;
  private int marginTime = 0;
  private Thread aliveNotifier = new Thread() {
    @Override
    public void run() {
      try {
        Files.createDirectories(ALIVE_NOTIFIER_PATH);
        for (File file : ALIVE_NOTIFIER_PATH.toFile().listFiles()) {
          if (file.isFile()) {
            file.delete();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      long previous = update(null);
      while (true) {
        try {
          sleep(1000);
          previous = update(previous);
        } catch (InterruptedException e) {
          ALIVE_NOTIFIER_PATH.resolve(String.valueOf(previous)).toFile().delete();
          return;
        }
      }
    }

    private long update(Long previous) {
      Long current = System.currentTimeMillis();
      try {
        if (previous != null) {
          ALIVE_NOTIFIER_PATH.resolve(previous.toString()).toFile().delete();
        }
        ALIVE_NOTIFIER_PATH.resolve(current.toString()).toFile().createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return current;
    }
  };

  public AbstractExecutor(int timeout, int marginTime) throws IOException {
    isAlive = true;
    aliveNotifier.start();
    this.timeout = timeout;
    this.marginTime = marginTime;
    Files.createDirectories(JOBS_PATH);
    Files.createDirectories(ENTITIES_PATH);

    try {
      if (!Files.exists(HASH_IGNORE_FILE_PATH)) {
        Files.createFile(HASH_IGNORE_FILE_PATH);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //fileWatchService = FileSystems.getDefault().newWatchService();
    //JOBS_PATH.register(fileWatchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
  }

  public void startPolling(Thread shutdownTimer) {
    this.shutdownTimer = shutdownTimer;
    /*
    Thread waitTimer = getWaitTimer();
    try {
      WatchKey watchKey = null;
      while ((watchKey = fileWatchService.take()) != null) {
        if (waitTimer != null) {
          waitTimer.interrupt();
          waitTimer = null;
        }

        for (WatchEvent<?> event : watchKey.pollEvents()) {
          System.out.println(event.kind().name() + " : " + event.context().toString());
        }

        checkJobs();
        watchKey.reset();

        if (runningJobList.isEmpty()) {
          waitTimer = getWaitTimer();
        }
      }
    } catch (InterruptedException | ClosedWatchServiceException e) {
      if (waitTimer != null) {
        waitTimer.interrupt();
      }
      return;
    }
     */
    while (isAlive && waitingTimeCounter <= timeout) {
      boolean isChanged = false;

      synchronized (runningJobList) {
        for (File file : JOBS_PATH.toFile().listFiles()) {
          if (!runningJobList.contains(file.getName())) {
            isChanged = true;
          }
        }
      }

      synchronized (runningJobList) {
        for (String id : runningJobList) {
          if (!JOBS_PATH.resolve(id).toFile().exists()) {
            isChanged = true;
          }
        }
      }

      if (isChanged) {
        checkJobs();
      }

      synchronized (runningJobList) {
        if (runningJobList.isEmpty()) {
          waitingTimeCounter += 1;
        } else {
          waitingTimeCounter = 0;
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        isAlive = false;
      }
    }

    if (isAlive) {
      shutdown();
    }
  }

  /*
  private Thread getWaitTimer() {
    Thread waitTimer = new Thread() {
      @Override
      public void run() {
        try {
          sleep(timeout * 1000);
          shutdown();
        } catch (InterruptedException e) {
        }
      }
    };
    waitTimer.start();
    return waitTimer;
  }
   */

  public void shutdown() {
    isAlive = false;
    System.err.println("Executor will shutdown (" + System.currentTimeMillis() + ")");

    try {
      Files.createFile(LOCKOUT_FILE_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*
    synchronized (objectLocker) {
      try {
        fileWatchService.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
     */

    checkJobs();

    for (int countDown = marginTime; countDown >= 0 && !runningJobList.isEmpty(); countDown--) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        //NP
      }
    }

    HashSet<String> temporaryJobList = new HashSet<>(runningJobList);
    for (String jobName : temporaryJobList) {
      if (Files.exists(JOBS_PATH.resolve(jobName))) {
        synchronized (runningJobList) {
          jobRemoved(jobName);
          runningJobList.remove(jobName);
        }
      }
    }

    if (shutdownTimer != null && shutdownTimer.isAlive()) {
      shutdownTimer.interrupt();
    }

    aliveNotifier.interrupt();
    System.err.println("Executor shutdown at " + System.currentTimeMillis());
  }

  protected void checkJobs() {
    synchronized (objectLocker) {
      HashSet<String> temporaryJobList = new HashSet<>(runningJobList);
      for (String jobName : temporaryJobList) {
        if (!Files.exists(JOBS_PATH.resolve(jobName))) {
          synchronized (runningJobList) {
            jobRemoved(jobName);
            runningJobList.remove(jobName);
          }
        }
      }
      for (File file : JOBS_PATH.toFile().listFiles()) {
        if (!runningJobList.contains(file.getName())) {
          synchronized (runningJobList) {
            jobAdded(file.getName());
            runningJobList.add(file.getName());
          }
        }
      }
    }
  }

  protected void jobAdded(String jobName) {
    System.out.println("'" + jobName + "' was added at " + System.currentTimeMillis());
  }

  protected void jobRemoved(String jobName) {
    System.out.println("'" + jobName + "' was forcibly removed at " + System.currentTimeMillis());
    try {
      synchronized (runningJobList) {
        Files.deleteIfExists(ENTITIES_PATH.resolve(jobName));
        Files.deleteIfExists(JOBS_PATH.resolve(jobName));
      }
    } catch (IOException e) {
    }
  }

  protected void jobFinished(String jobName) {
    synchronized (objectLocker) {
      synchronized (runningJobList) {
        runningJobList.remove(jobName);
        try {
          Files.deleteIfExists(ENTITIES_PATH.resolve(jobName));
          Files.deleteIfExists(JOBS_PATH.resolve(jobName));
        } catch (IOException e) {
        }
      }
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
          Path entityPath = ENTITIES_PATH.resolve(jobName);
          while (!Files.exists(entityPath)) {
            System.out.println(entityPath.toString() + " is not exist; wait a second and retry.");
            TimeUnit.SECONDS.sleep(1);
          }
          Path directoryPath = Paths.get(Files.readString(entityPath).trim());
          while (!Files.isWritable(directoryPath)) {
            System.out.println(directoryPath.toString() + " is not writable; wait a second and retry.");
            TimeUnit.SECONDS.sleep(1);
          }

          process = new ProcessBuilder().directory(directoryPath.toFile())
            .command("sh", BATCH_FILE).redirectErrorStream(true).start();

          Thread inputStreamThread = new Thread() {
            boolean isAlive = true;
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            @Override
            public void run() {
              try {
                while (isAlive) {
                  String line = reader.readLine();
                  if (line == null) {
                    isAlive = false;
                  } else {
                    System.out.println(line);
                  }
                }
              } catch (IOException e) {
                throw  new RuntimeException(e);
              } finally {
                try {
                  reader.close();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            }
          };
          inputStreamThread.start();

          process.waitFor();
        } catch (Exception e) {
          System.err.println("'" + jobName + "' was failed to execute at " + System.currentTimeMillis());
          e.printStackTrace();
        }

        jobFinished(jobName);

        System.out.println("'" + jobName + "' finished at " + System.currentTimeMillis());
      }

      @Override
      public void interrupt() {
        isCanceled = true;
        if (process != null) {
          process.destroyForcibly();
          System.err.println("'" + jobName + "' canceled at " + System.currentTimeMillis());
        }
      }
    };

    thread.start();


    return thread;
  }
}
