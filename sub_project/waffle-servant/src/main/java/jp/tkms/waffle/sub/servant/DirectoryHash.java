package jp.tkms.waffle.sub.servant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class DirectoryHash {
  public static final String HASH_FILE = ".WAFFLE_HASH";
  public static final String IGNORE_FLAG = ".WAFFLE_HASH_IGNORE";
  private static final String SEPARATOR = ":";

  Path baseDirectory;
  Path directoryPath;
  byte[] hash = null;

  public DirectoryHash(Path baseDirectory, Path directoryPath, boolean isReady) {
    this.baseDirectory = baseDirectory;
    if (!directoryPath.isAbsolute()) {
      this.directoryPath = baseDirectory.resolve(directoryPath).normalize();
    } else {
      this.directoryPath = directoryPath.normalize();
    }

    if (isReady) {
      calculate();
    }
  }

  public DirectoryHash(Path baseDirectory, Path directoryPath) {
    this(baseDirectory, directoryPath, true);
  }

  public byte[] getHash() {
    if (hash == null) {
      calculate();
    }
    return hash;
  }

  public void calculate() {
    TreeSet<String> fileSet = new TreeSet<>();
    collectFilesStatusTo(fileSet, directoryPath);
    StringBuilder chainedStatus = new StringBuilder();
    for (String s : fileSet) {
      chainedStatus.append(s);
      chainedStatus.append(SEPARATOR);
    }

    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      hash = sha256.digest(chainedStatus.toString().getBytes());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public void collectFilesStatusTo(TreeSet<String> fileSet, Path target) {
    boolean hasIgnoreFlag = false;
    try (Stream<Path> stream = Files.list(target)) {
      hasIgnoreFlag = !stream.noneMatch(p -> p.getFileName().toString().equals(IGNORE_FLAG));
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!hasIgnoreFlag) {
      try (Stream<Path> stream = Files.list(target)) {
        stream.forEach(p -> {
          if (Files.isSymbolicLink(p)) {
            // NOP
          } else if (Files.isDirectory(p)) {
            collectFilesStatusTo(fileSet, p);
          } else if (!p.toFile().getName().equals(HASH_FILE) && !p.toFile().getName().endsWith(IGNORE_FLAG)) {
            try {
              fileSet.add(baseDirectory.relativize(p).normalize().toString() + SEPARATOR + Files.size(p));
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

  public boolean hasHashFile() {
    return Files.exists(directoryPath.resolve(HASH_FILE));
  }

  public boolean isMatchToHashFile() {
    return Arrays.equals(readHashFile(directoryPath), getHash());
  }

  public boolean waitToMatch(int timeout) {
    boolean isMatched = false;
    int count = 0;
    while (!(isMatched = isMatchToHashFile()) && count < timeout) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        // NOP
      }
      calculate();
      if (timeout >= 0) {
        count += 1;
      }
    }
    return isMatched;
  }

  public void save() {
    try (FileOutputStream stream = new FileOutputStream(directoryPath.resolve(HASH_FILE).toFile(), false)) {
      stream.write(getHash());
      stream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean update() {
    if (hasHashFile()) {
      calculate();
      if (!isMatchToHashFile()) {
        save();
        return true;
      }
    } else {
      save();
      return true;
    }
    return false;
  }

  public static byte[] readHashFile(Path directoryPath) {
    try {
      return Files.readAllBytes(directoryPath.resolve(HASH_FILE));
    } catch (IOException e) {
      return null;
    }
  }
}
