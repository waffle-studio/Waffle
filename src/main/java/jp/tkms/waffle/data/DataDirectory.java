package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.HasLocalPath;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.TimedMap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public interface DataDirectory extends HasLocalPath {
  int EOF = -1;
  int CHECKING_OMIT_TIME = 30000;
  TimedMap<String, Long> previousDifferenceCheckTimeMap = new TimedMap<>(CHECKING_OMIT_TIME / 1000);

  default void createNewFile(Path path) {
    synchronized (this) {
      if (!path.isAbsolute()) {
        path = getPath().resolve(path);
      }
      if (!Files.exists(path)) {
        try {
          Files.createDirectories(getPath());
          path.toFile().createNewFile();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  default String getFileContents(Path path) {
    synchronized (this) {
      if (!path.isAbsolute()) {
        path = getPath().resolve(path);
      }
      return StringFileUtil.read(path);
    }
  }

  default void updateFileContents(Path path, String contents) {
    synchronized (this) {
      if (!path.isAbsolute()) {
        path = getPath().resolve(path);
      }
      if (Files.exists(path)) {
        StringFileUtil.write(path, contents);
      }
    }
  }

  default void appendFileContents(Path path, String contents) {
    synchronized (this) {
      if (!path.isAbsolute()) {
        path = getPath().resolve(path);
      }
      if (Files.exists(path)) {
        StringFileUtil.append(path, contents);
      }
    }
  }

  default void createNewFile(String fileName) {
    createNewFile(Paths.get(fileName));
  }

  default String getFileContents(String fileName) {
    return getFileContents(Paths.get(fileName));
  }

  default void updateFileContents(String fileName, String contents) {
    updateFileContents(Paths.get(fileName), contents);
  }

  default void appendFileContents(String fileName, String contents) {
    appendFileContents(Paths.get(fileName), contents);
  }

  default String getDirectoryName() {
    return getPath().getFileName().toString();
  }

  default void copyDirectory(Path dest) throws IOException {
    copyDirectory(getPath().toFile(), dest.toFile());
  }

  static void copyDirectory(File src, File dest) throws IOException {
    if (src.isDirectory()) {
      if (!dest.exists()) {
        Files.createDirectories(dest.toPath());
      }
      String files[] = src.list();
      for (String file : files) {
        File srcFile = new File(src, file);
        File destFile = new File(dest, file);
        copyDirectory(srcFile, destFile);
      }
      dest.setLastModified(src.lastModified());
    }else{
      Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
      dest.setLastModified(src.lastModified());
    }
  }

  default void deleteDirectory() {
    deleteDirectory(getPath().toFile());
  }

  static void deleteDirectory(File target) {
    if (target.isDirectory() && !Files.isSymbolicLink(target.toPath())) {
      for (File file : target.listFiles()) {
        deleteDirectory(file);
      }
    }
    target.delete();
  }

  default boolean hasNotDifference(DataDirectory target, Path... ignoringPaths) {
    Long previousCheckTime = previousDifferenceCheckTimeMap.get(getPath().toString());

    if (target != null && Files.isDirectory(getPath()) && Files.isDirectory(target.getPath())) {
      HashMap<Path, File> ownFileMap = getFileMap(getPath(), getPath().toFile());
      HashMap<Path, File> targetFileMap = getFileMap(target.getPath(), target.getPath().toFile());

      for (Path path : ignoringPaths) {
        ownFileMap.remove(path);
        targetFileMap.remove(path);
      }

      if (ownFileMap.size() != targetFileMap.size()) {
        return false;
      }

      previousDifferenceCheckTimeMap.put(getPath().toString(), Long.valueOf(System.currentTimeMillis()));

      if (previousCheckTime != null && previousCheckTime.longValue() + CHECKING_OMIT_TIME > System.currentTimeMillis() ) {
        boolean detected = false;
        for (Map.Entry<Path, File> entry : ownFileMap.entrySet()) {
          if (entry.getKey().equals(Paths.get(".").normalize())) {
            continue;
          }

          File ownFile = entry.getValue();
          File targetFile = targetFileMap.get(entry.getKey());

          if (targetFile == null || !ownFile.exists() || !targetFile.exists() ||
            ownFile.lastModified() != targetFile.lastModified() ||
            ownFile.length() != targetFile.length()
          ) {
            detected = true;
            break;
          }
        }
        if (!detected) {
          return true;
        }
      }

      try {
        ExecutorService executorService = Executors.newWorkStealingPool();
        ArrayList<Future<Boolean>> futureList = new ArrayList<>();
        for (Map.Entry<Path, File> entry : ownFileMap.entrySet()) {
          futureList.add(executorService.submit(new CompareFiles(entry.getValue(), targetFileMap.get(entry.getKey()))));
        }

        for (Future<Boolean> future : futureList) {
          if (!future.get()) {
            executorService.shutdownNow();
            return false;
          }
        }

        executorService.shutdown();
      } catch (Throwable t) {
        return false;
      }

      return true;
    }

    return false;
  }

  static HashMap getFileMap(Path rootPath, File target) {
    HashMap<Path, File> fileMap = new HashMap<>();
    fileMap.put(rootPath.normalize().relativize(target.toPath().normalize()), target);
    if (target.isDirectory()) {
      for (File file : target.listFiles()) {
        if (file.isDirectory()) {
          fileMap.putAll(getFileMap(rootPath, file));
        } else {
          fileMap.put(rootPath.normalize().relativize(file.toPath().normalize()), file);
        }
      }
    }
    return fileMap;
  }

  class CompareFiles implements Callable<Boolean> {
    File ownFile;
    File targetFile;

    public CompareFiles(File ownFile, File targetFile) {
      this.ownFile = ownFile;
      this.targetFile = targetFile;
    }

    @Override
    public Boolean call() throws Exception {
      if (targetFile == null || !ownFile.exists() || !targetFile.exists()) {
        return false;
      }
      if (ownFile.isDirectory()) {
        if (!targetFile.isDirectory()) {
          return false;
        }
        return true;
      }
      if (!Files.isReadable(ownFile.toPath()) || !Files.isReadable(targetFile.toPath())) {
        return false;
      }
      if (ownFile.length() != targetFile.length()) {
        return false;
      }
      try (
        InputStream ownFileStream = new FileInputStream(ownFile);
        InputStream targetFileStream = new FileInputStream(targetFile);
      ) {
        BufferedInputStream ownFileBufferedStream = new BufferedInputStream(ownFileStream);
        BufferedInputStream targetFileBufferedStream = new BufferedInputStream(targetFileStream);
        for (int ch = ownFileBufferedStream.read(); EOF != ch; ch = ownFileBufferedStream.read()) {
          if (ch != targetFileBufferedStream.read()) {
            return false;
          }
        }
        if (targetFileBufferedStream.read() != EOF) {
          return false;
        }
        ownFileBufferedStream.close();
        targetFileBufferedStream.close();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      return true;
    }
  }
}
