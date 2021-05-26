package jp.tkms.waffle.sub.servant;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class DirectoryHash {
  public static final String HASH_FILE = ".WAFFLE_HASH";
  private static final String SEPARATOR = ".WAFFLE_HASH";

  Path baseDirectory;
  Path directoryPath;
  byte[] hash;

  public DirectoryHash(Path baseDirectory, Path directoryPath) {
    this.baseDirectory = baseDirectory;
    if (!directoryPath.isAbsolute()) {
      this.directoryPath = baseDirectory.resolve(directoryPath).normalize();
    }
    this.directoryPath = directoryPath.normalize();

    calculate();
  }

  public byte[] getHash() {
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
    try (Stream<Path> stream = Files.list(target)) {
      stream.forEach(p -> {
        if (Files.isDirectory(p)) {
          collectFilesStatusTo(fileSet, p);
        } else if (!p.toFile().getName().equals(HASH_FILE)) {
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

  public boolean isMatchToHashFile() {
    return readHashFile(directoryPath).equals(hash);
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
      if (timeout >= 0) {
        count += 1;
      }
    }
    return isMatched;
  }

  public void save() {
    try (FileOutputStream stream = new FileOutputStream(directoryPath.resolve(HASH_FILE).toFile(), false)) {
      stream.write(hash);
      stream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static byte[] readHashFile(Path directoryPath) {
    try {
      return Files.readAllBytes(directoryPath.resolve(HASH_FILE));
    } catch (IOException e) {
      return new byte[0];
    }
  }
}
