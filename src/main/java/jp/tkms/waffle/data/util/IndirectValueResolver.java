package jp.tkms.waffle.data.util;

public class IndirectValueResolver {
  String getString(String defaults) {
    return null;
  }

  String getString() {
    return getString(null);
  }

  public static IndirectValueResolver resolve(String key) {
    return new IndirectValueResolver();
  }
}
