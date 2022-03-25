package jp.tkms.waffle.data.util;

import jp.tkms.waffle.Constants;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileName {
  public static String removeRestrictedCharacters(String name) {
    return name.replaceAll("[^0-9a-zA-Z_.,+\\-]", "_").trim();
    //return name.replaceAll("[^0-9a-zA-Z ()_.,+\\-]", "_").trim();
  }

  public static String generateUniqueFileName(Path basePath, String name) {
    int count = 0;
    int padding = 1;
    boolean isAutoIndexing = false;
    String result;
    if (name.length() <= 0) {
      result = "_" + count;
    } else if (name.endsWith("@")) {
      String atRemoved = name.replaceFirst("@+$", "");
      padding = name.length() - atRemoved.length();
      name = FileName.removeRestrictedCharacters(atRemoved);
      result = String.format("%s%0" + padding + "d", name, count);
    } else {
      count = 1;
      isAutoIndexing = true;
      name = FileName.removeRestrictedCharacters(name);
      result = name;
    }
    while (result.length() <= 0 || Files.exists(basePath.resolve(result))) {
      count += 1;
      result = String.format("%s" + (isAutoIndexing ? "_" : "") + "%0" + padding + "d", name, count);
    }
    return result;
  }

  public static Path generateUniqueFilePath(Path path) {
    Path parent = path.getParent();
    if (path.isAbsolute()) {
      return parent.resolve(generateUniqueFileName(parent, path.getFileName().toString())).normalize();
    } else {
      return parent.resolve(generateUniqueFileName(Constants.WORK_DIR.resolve(parent), path.getFileName().toString())).normalize();
    }
  }
}
