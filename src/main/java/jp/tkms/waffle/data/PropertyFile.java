package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.data.util.WrappedJsonArray;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public interface PropertyFile {
  WrappedJson getPropertyStoreCache();
  void setPropertyStoreCache(WrappedJson cache);

  default Path getPropertyStorePath() {
    return Paths.get("waffle.json");
  }

  private Object getLockerObject() {
    return this;
  }

  private Path getAbsolutePropertyStorePath() {
    Path path = getPropertyStorePath();
    if (path.isAbsolute()) {
      return path;
    } else {
      return Constants.WORK_DIR.resolve(path);
    }
  }

  private WrappedJson getPropertyStore() {
    synchronized (getLockerObject()) {
      WrappedJson cache = getPropertyStoreCache();
      if (cache == null) {
        Path storePath = getAbsolutePropertyStorePath();
        String json = "{}";
        if (Files.exists(storePath)) {
          json = StringFileUtil.read(storePath);
        }
        try {
          if (!"".equals(json)) {
            cache = new WrappedJson(json);
          }
        }catch (Exception e) {
          WarnLogMessage.issue(storePath.toString() + " is broken : " + json);
        }
        if (cache == null) {
          cache = new WrappedJson();
        }
        setPropertyStoreCache(cache);
      }
      return cache;
    }
  }

  default void reloadPropertyStore() {
    synchronized (getLockerObject()) {
      setPropertyStoreCache(null);
    }
  }

  private void updatePropertyStore() {
    synchronized (getLockerObject()) {
      if (getPropertyStoreCache() != null) {
        getPropertyStoreCache().writePrettyFile(getAbsolutePropertyStorePath());
      }
    }
  }

  default String getStringFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getString(key, null);
    }
  }

  default String getStringFromProperty(String key, String defaultValue) {
    synchronized (getLockerObject()) {
      String value = getStringFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default Boolean getBooleanFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getBoolean(key, null);
    }
  }

  default Boolean getBooleanFromProperty(String key, Boolean defaultValue) {
    synchronized (getLockerObject()) {
      Boolean value = getBooleanFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default Integer getIntFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getInt(key, null);
    }
  }

  default Integer getIntFromProperty(String key, Integer defaultValue) {
    synchronized (getLockerObject()) {
      Integer value = getIntFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default Long getLongFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getLong(key, null);
    }
  }

  default Long getLongFromProperty(String key, Long defaultValue) {
    synchronized (getLockerObject()) {
      Long value = getLongFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default Double getDoubleFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getDouble(key, null);
    }
  }

  default Double getDoubleFromProperty(String key, Double defaultValue) {
    synchronized (getLockerObject()) {
      Double value = getDoubleFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default WrappedJson getObjectFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getObject(key, null);
    }
  }

  default WrappedJson getObjectFromProperty(String key, WrappedJson defaultValue) {
    synchronized (getLockerObject()) {
      WrappedJson value = getObjectFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default WrappedJsonArray getArrayFromProperty(String key) {
    synchronized (getLockerObject()) {
      return getPropertyStore().getArray(key, null);
    }
  }

  default WrappedJsonArray getArrayFromProperty(String key, WrappedJsonArray defaultValue) {
    synchronized (getLockerObject()) {
      WrappedJsonArray value = getArrayFromProperty(key);
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }
  }

  default void removeFromProperty(String key) {
    synchronized (getLockerObject()) {
      getPropertyStore().remove(key);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, String value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, boolean value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, int value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, long value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, double value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, WrappedJson value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, WrappedJsonArray value) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void putNewArrayToProperty(String key) {
    synchronized (getLockerObject()) {
      getPropertyStore().put(key, new ArrayList<>());
      updatePropertyStore();
    }
  }

  default void putToArrayOfProperty(String key, String value) {
    synchronized (getLockerObject()) {
      WrappedJsonArray array = getPropertyStore().getArray(key, null);
      if (array == null) {
        array = new WrappedJsonArray();
        getPropertyStore().put(key, array);
      }
      if (!array.contains(value)) {
        array.add(value);
      }
      updatePropertyStore();
    }
  }

  default void removeFromArrayOfProperty(String key, Object value) {
    synchronized (getLockerObject()) {
      getPropertyStore().getArray(key, new WrappedJsonArray()).remove(value);
      updatePropertyStore();
    }
  }
}
