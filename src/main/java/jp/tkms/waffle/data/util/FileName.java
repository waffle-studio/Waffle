package jp.tkms.waffle.data.util;

public class FileName {
  public static String removeRestrictedCharacters(String name) {
    return name.replaceAll("[^0-9a-zA-Z ()_.,+\\-]", "_").trim();
  }
}
