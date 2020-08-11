package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileBuffer extends Thread {
  private Path path;
  private String contents;
  private boolean isReadMode;
  private boolean alive = true;
  private long timestamp;

  private static ExecutorService threadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  private static Lock bufferLock = new ReentrantLock();
  private static boolean flushFlag = false;
  private static ConcurrentHashMap<Path, FileBuffer> map = new ConcurrentHashMap<>();
  private static ConcurrentLinkedQueue<FileBuffer> queue = new ConcurrentLinkedQueue<>();
  private static HashSet<FileBuffer> runningSet = new HashSet<>();

  public static void write(Path path, String contents) {
    try {
      Path directoryPath = path.getParent();
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }

      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write(contents);
      filewriter.close();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    /*
    checkFlushFlag();
    runLifeCycle();
    synchronized (map) {
      Path absolutePath = path.toAbsolutePath();
      FileBuffer fileBuffer = map.get(absolutePath);
      if (fileBuffer != null && fileBuffer.alive) {
        fileBuffer.setContents(contents);
        if (fileBuffer.isReadMode) {
          threadPool.submit(fileBuffer);
          fileBuffer.isReadMode = false;
        }
        return;
      }
      fileBuffer = new FileBuffer(absolutePath, contents);
      map.put(absolutePath, fileBuffer);
      queue.offer(fileBuffer);
      if (Files.exists(absolutePath)) {
        threadPool.submit(fileBuffer);
      } else {
        fileBuffer.writeToDisk();
        fileBuffer.isReadMode = true;
      }
    }

     */
  }

  public static String read(Path path) {
    try {
      return new String(Files.readAllBytes(path));
    } catch (IOException e) {
      return "";
    }
    /*
    checkFlushFlag();
    runLifeCycle();
    FileBuffer fileBuffer = null;
    synchronized (map) {
      Path absolutePath = path.toAbsolutePath();
      fileBuffer = map.get(absolutePath);
      if (fileBuffer == null) {
        fileBuffer = new FileBuffer(absolutePath);
        map.put(absolutePath, fileBuffer);
        queue.offer(fileBuffer);
      }
    }
    return fileBuffer.getContents();

     */
  }

  public static void flush() {
    bufferLock.lock();
    try {
      flushFlag = true;
      synchronized (map) {
        for (FileBuffer fileBuffer : queue) {
          if (!fileBuffer.isReadMode) {
            fileBuffer.interrupt();
          }
        }
        map.clear();
        queue.clear();
      }
      int runningCount = 0;
      do {
        synchronized (runningSet) {
          runningCount = runningSet.size();
        }
      } while (runningCount > 0);
      flushFlag = false;
    } finally {
      bufferLock.unlock();
    }
  }

  public static void shutdown() {
    try {
      flush();
      threadPool.shutdown();
      threadPool.awaitTermination(7, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      ErrorLogMessage.issue(e);
    }
  }

  private static void checkFlushFlag() {
    bufferLock.lock();
    bufferLock.unlock();
  }

  private static void runLifeCycle() {
    synchronized (map) {
      while (!queue.isEmpty() && queue.peek().isReadMode && queue.peek().timestamp + 1000 > System.currentTimeMillis()) {
        FileBuffer fileBuffer = queue.poll();
        map.remove(fileBuffer.path);
      }
    }
  }

  private FileBuffer(Path path, String contents) {
    super(FileBuffer.class.getSimpleName());
    this.path = path;
    this.contents = contents;
    this.isReadMode = false;
    this.timestamp = System.currentTimeMillis();
  }

  private FileBuffer(Path path) {
    super(FileBuffer.class.getSimpleName());
    this.path = path;
    try {
      this.contents = new String(Files.readAllBytes(path));
    } catch (IOException e) {
      this.contents = "";
    }
    this.isReadMode = true;
    this.timestamp = System.currentTimeMillis();
  }

  private String getContents() {
    synchronized (this) {
      return contents;
    }
  }

  private void setContents(String contents) {
    synchronized (this) {
      this.contents = contents;
    }
    synchronized (map) {
      this.timestamp = System.currentTimeMillis();
      queue.remove(this);
      queue.add(this);
    }
  }

  private void writeToDisk() {
    try {
      Path directoryPath = path.getParent();
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }

      FileWriter filewriter = new FileWriter(path.toFile());
      filewriter.write(contents);
      filewriter.close();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  @Override
  public void run() {
    synchronized (runningSet) {
      runningSet.add(this);
    }
    while (timestamp + 1000 > System.currentTimeMillis() && !isInterrupted()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) { }
    }
    if (!flushFlag) {
      synchronized (map) {
        map.remove(path);
        queue.remove(this);
        alive = false;
      }
    }
    writeToDisk();
    synchronized (runningSet) {
      runningSet.remove(this);
    }
  }
}
