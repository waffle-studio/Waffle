package jp.tkms.waffle.data.internal;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.ResourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ServantJarFile {
  public static final String JAR_FILE = "waffle-servant-all.jar";
  private static final String JAR_RESOURCE = "/" + JAR_FILE + ".bin";
  private static final Path JAR_PATH = Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(JAR_FILE);
  private static Object objectLocker = new Object();

  public static Path getPath() {
    if (!Files.exists(JAR_PATH)) {
      synchronized (objectLocker) {
        if (!Files.exists(JAR_PATH)) {
          ResourceFile.copyFile(JAR_RESOURCE, JAR_PATH);
          JAR_PATH.toFile().deleteOnExit();
        }
      }
    }
    return JAR_PATH;
  }

  public static String getMD5Sum() {
    StringBuilder hash = new StringBuilder();
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      for (byte b : md5.digest(Files.readAllBytes(getPath()))) {
        hash.append(String.format("%02x", b));
      }
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }
    return hash.toString();
  }
}
