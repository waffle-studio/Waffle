package jp.tkms.waffle.data.util;

import java.util.AbstractMap;

public class KeyValue extends AbstractMap.SimpleEntry<String, String> {
  public KeyValue(String key, String value) {
    super(key, value);
  }
}
