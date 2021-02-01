package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class PathSemaphore extends Semaphore {
  private static ConcurrentHashMap<Path, PathSemaphore> semaphoreMap = new ConcurrentHashMap<>();

  private Path normalizedPath;

  public PathSemaphore(Path normalizedPath) {
    super(1);
    this.normalizedPath = normalizedPath;
  }

  public static PathSemaphore acquire(Path path) {
    Path normalizedPath = path.toAbsolutePath().normalize();
    PathSemaphore semaphore = semaphoreMap.get(normalizedPath);
    if (semaphore == null) {
      synchronized (semaphoreMap) {
        semaphore = semaphoreMap.get(normalizedPath);
        if (semaphore == null) {
          semaphore = new PathSemaphore(normalizedPath);
          semaphoreMap.put(normalizedPath, semaphore);
        }
      }
    }
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      ErrorLogMessage.issue(e);
    }
    return semaphore;
  }

  @Override
  public void release() {
    super.release();
    if (!hasQueuedThreads()) {
      synchronized (semaphoreMap) {
        if (!hasQueuedThreads()) {
          semaphoreMap.remove(normalizedPath);
        }
      }
    }
  }
}
