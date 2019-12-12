package jp.tkms.util;

public class Values {
  public static Object convertString(String s) {
    try {
      if (s.indexOf('.') < 0) {
        return Integer.parseInt(s);
      } else {
        return Double.parseDouble(s);
      }
    } catch (NumberFormatException e) {
      return s;
    }
  }
}
