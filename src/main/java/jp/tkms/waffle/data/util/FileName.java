package jp.tkms.waffle.data.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileName {
  public static String removeRestrictedCharacters(String name) {
    return name.replaceAll("[^0-9a-zA-Z_.,+\\-]", "_").trim();
    //return name.replaceAll("[^0-9a-zA-Z ()_.,+\\-]", "_").trim();
  }

  public static String generateUniqueFileName(Path basePath, String name) {
    name = FileName.removeRestrictedCharacters(name);
    String result = name;
    int count = 1;
    if (result.length() <= 0) {
      count = 0;
    }
    while (result.length() <= 0 || Files.exists(basePath.resolve(result))) {
      result = name + '_' + count++;
    }
    return result;
  }
}
