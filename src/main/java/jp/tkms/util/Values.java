package jp.tkms.util;

public class Values {
  public static Object convertString(String s) {
    try {
      if (s.indexOf('.') < 0) {
        return Long.parseLong(s);
      } else {
        return Double.parseDouble(s);
      }
    } catch (NumberFormatException e) {
      return s;
    }
  }
}
