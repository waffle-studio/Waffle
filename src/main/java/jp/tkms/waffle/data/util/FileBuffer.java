package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class FileBuffer extends Thread {
  private Path path;
  private String contents;
  private boolean isReadMode;
  private boolean alive = true;
  private long timestamp;

  private static ExecutorService threadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new LinkedBlockingQueue());
  private static ConcurrentHashMap<Path, FileBuffer> map = new ConcurrentHashMap<>();
  private static ConcurrentLinkedQueue<FileBuffer> queue = new ConcurrentLinkedQueue<>();

  public static void write(Path path, String contents) {
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
      threadPool.submit(fileBuffer);
    }
  }

  public static String read(Path path) {
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
  }

  public static void shutdown() {
    try {
      threadPool.shutdown();
      threadPool.awaitTermination(7, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      ErrorLogMessage.issue(e);
    }
  }

  private static void runLifeCycle() {
    synchronized (map) {
      while (!queue.isEmpty() && queue.peek().timestamp + 1000 > System.currentTimeMillis()) {
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

  @Override
  public void run() {
    while (timestamp + 1000 > System.currentTimeMillis()) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) { }
    }
    synchronized (map) {
      map.remove(path);
      queue.remove(this);
      alive = false;
    }
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
}
