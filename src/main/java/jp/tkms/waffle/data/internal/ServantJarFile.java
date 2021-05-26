package jp.tkms.waffle.data.internal;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.ResourceFile;

import java.nio.file.Files;
import java.nio.file.Path;

public class ServantJarFile {
  private static final String JAR_FILE = "waffle-servant-all.jar";
  private static final Path JAR_PATH = Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(JAR_FILE);
  private static Object objectLocker = new Object();

  public static Path getPath() {
    if (!Files.exists(JAR_PATH)) {
      synchronized (objectLocker) {
        if (!Files.exists(JAR_PATH)) {
          ResourceFile.copyFile('/' + JAR_FILE, JAR_PATH);
        }
      }
    }
    return JAR_PATH;
  }
}
