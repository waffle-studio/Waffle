package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.StringFileUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public interface PropertyFile {
  JSONObject getPropertyStoreCache();
  void setPropertyStoreCache(JSONObject cache);

  default Path getPropertyStorePath() {
    return Paths.get("waffle.json");
  }

  private JSONObject getPropertyStore() {
    synchronized (this) {
      JSONObject cache = getPropertyStoreCache();
      if (cache == null) {
        Path storePath = getPropertyStorePath();
        String json = "{}";
        if (Files.exists(storePath)) {
          json = StringFileUtil.read(storePath);
        }
        try {
          if (!"".equals(json)) {
            cache = new JSONObject(json);
          }
        }catch (Exception e) {
          WarnLogMessage.issue(storePath.toString() + " is broken : " + json);
        }
        if (cache == null) {
          cache = new JSONObject("{}");
        }
        setPropertyStoreCache(cache);
      }
      return cache;
    }
  }

  default void reloadPropertyStore() {
    synchronized (this) {
      setPropertyStoreCache(null);
    }
  }

  private void updatePropertyStore() {
    synchronized (this) {
      if (getPropertyStoreCache() != null) {
        Path storePath = getPropertyStorePath();
        StringFileUtil.write(storePath, getPropertyStoreCache().toString(2));
      }
    }
  }

  default String getStringFromProperty(String key) {
    synchronized (this) {
      try {
        return getPropertyStore().getString(key);
      } catch (Exception e) {
      }
      return null;
    }
  }

  default String getStringFromProperty(String key, String defaultValue) {
    synchronized (this) {
      String value = getStringFromProperty(key);
      if (value == null) {
        value = defaultValue;
        if (value != null) {
          setToProperty(key, defaultValue);
        }
      }
      return value;
    }
  }

  default Integer getIntFromProperty(String key) {
    synchronized (this) {
      try {
        return getPropertyStore().getInt(key);
      } catch (Exception e) {
      }
      return null;
    }
  }

  default Integer getIntFromProperty(String key, Integer defaultValue) {
    synchronized (this) {
      Integer value = getIntFromProperty(key);
      if (value == null) {
        value = defaultValue;
        if (value != null) {
          setToProperty(key, defaultValue);
        }
      }
      return value;
    }
  }

  default Long getLongFromProperty(String key) {
    synchronized (this) {
      try {
        return getPropertyStore().getLong(key);
      } catch (Exception e) {
      }
      return null;
    }
  }

  default Long getLongFromProperty(String key, Long defaultValue) {
    synchronized (this) {
      Long value = getLongFromProperty(key);
      if (value == null) {
        value = defaultValue;
        if (value != null) {
          setToProperty(key, defaultValue);
        }
      }
      return value;
    }
  }

  default Double getDoubleFromProperty(String key) {
    synchronized (this) {
      try {
        return getPropertyStore().getDouble(key);
      } catch (Exception e) {
      }
      return null;
    }
  }

  default Double getDoubleFromProperty(String key, Double defaultValue) {
    synchronized (this) {
      Double value = getDoubleFromProperty(key);
      if (value == null) {
        value = defaultValue;
        if (value != null) {
          setToProperty(key, defaultValue);
        }
      }
      return value;
    }
  }

  default JSONObject getJSONObjectFromProperty(String key) {
    synchronized (this) {
      try {
        return getPropertyStore().getJSONObject(key);
      } catch (Exception e) {
      }
      return null;
    }
  }

  default JSONObject getJSONObjectFromProperty(String key, JSONObject defaultValue) {
    synchronized (this) {
      JSONObject value = getJSONObjectFromProperty(key);
      if (value == null) {
        value = defaultValue;
        if (value != null) {
          setToProperty(key, defaultValue);
        }
      }
      return value;
    }
  }

  default JSONArray getArrayFromProperty(String key) {
    synchronized (this) {
      return getPropertyStore().getJSONArray(key);
    }
  }

  default void removeFromProperty(String key) {
    synchronized (this) {
      getPropertyStore().remove(key);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, String value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, int value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, long value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, double value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, JSONObject value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void setToProperty(String key, JSONArray value) {
    synchronized (this) {
      getPropertyStore().put(key, value);
      updatePropertyStore();
    }
  }

  default void putNewArrayToProperty(String key) {
    synchronized (this) {
      getPropertyStore().put(key, new ArrayList<>());
      updatePropertyStore();
    }
  }

  default void putToArrayOfProperty(String key, String value) {
    synchronized (this) {
      JSONArray array = null;
      boolean isContain = false;
      try {
        array = getPropertyStore().getJSONArray(key);
        isContain = array.toList().contains(value);
      } catch (Exception e) {
      }
      if (array == null) {
        getPropertyStore().put(key, new ArrayList<>());
        array = getPropertyStore().getJSONArray(key);
      }
      if (!isContain) {
        array.put(value);
      }
      updatePropertyStore();
    }
  }

  default void removeFromArrayOfProperty(String key, String value) {
    synchronized (this) {
      try {
        JSONArray array = getPropertyStore().getJSONArray(key);
        int index = array.toList().indexOf(value);
        if (index >= 0) {
          array.remove(index);
        }
      } catch (Exception e) {
      }
      updatePropertyStore();
    }
  }
}
