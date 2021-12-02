package jp.tkms.waffle.data.util;

import java.util.HashMap;

public class StringKeyHashMap<V> extends HashMap<String, V> {
  public static final String EMPTY_KEY_PREFIX = "#";

  private int emptyKeyCount = 0;

  public static String toEmptyKey(int index) {
    return EMPTY_KEY_PREFIX + index;
  }

  public V get() {
    return get(toEmptyKey(0));
  }

  public V put(V value) {
    return put(toEmptyKey(emptyKeyCount++), value);
  }
}
