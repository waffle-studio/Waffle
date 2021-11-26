package jp.tkms.waffle.data.util;

import java.util.HashMap;

public class StringKeyHashMap<V> extends HashMap<String, V> {
  public static final String EMPTY_KEY = "";

  public V get() {
    return get(EMPTY_KEY);
  }

  public V put(V value) {
    return put(EMPTY_KEY, value);
  }
}
