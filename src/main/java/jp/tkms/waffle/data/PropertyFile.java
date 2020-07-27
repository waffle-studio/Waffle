package jp.tkms.waffle.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
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
      if (getPropertyStoreCache() == null) {
        Path storePath = getPropertyStorePath();
        String json = "{}";
        if (Files.exists(storePath)) {
          try {
            json = new String(Files.readAllBytes(storePath));
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        try {
          setPropertyStoreCache(new JSONObject(json));
        }catch (Exception e) {
          System.err.println(json);
        }
      }
    }
    return getPropertyStoreCache();
  }

  default void reloadPropertyStore() {
    synchronized (this) {
      setPropertyStoreCache(null);
    }
  }

  private void updatePropertyStore() {
    synchronized (this) {
      if (getPropertyStoreCache() != null) {
        Path directoryPath = getPropertyStorePath().getParent();
        if (!Files.exists(directoryPath)) {
          try {
            Files.createDirectories(directoryPath);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        Path storePath = getPropertyStorePath();
        try {
          FileWriter filewriter = new FileWriter(storePath.toFile());
          filewriter.write(getPropertyStoreCache().toString(2));
          filewriter.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  default String getStringFromProperty(String key) {
    try {
      return getPropertyStore().getString(key);
    } catch (Exception e) {}
    return null;
  }

  default String getStringFromProperty(String key, String defaultValue) {
    String value = getStringFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  default Integer getIntFromProperty(String key) {
    try {
      return getPropertyStore().getInt(key);
    } catch (Exception e) {}
    return null;
  }

  default Integer getIntFromProperty(String key, Integer defaultValue) {
    Integer value = getIntFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  default Long getLongFromProperty(String key) {
    try {
      return getPropertyStore().getLong(key);
    } catch (Exception e) {}
    return null;
  }

  default JSONObject getJSONObjectFromProperty(String key) {
    try {
      return getPropertyStore().getJSONObject(key);
    } catch (Exception e) {}
    return null;
  }

  default JSONObject getJSONObjectFromProperty(String key, JSONObject defaultValue) {
    JSONObject value = getJSONObjectFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  default JSONArray getArrayFromProperty(String key) {
    return getPropertyStore().getJSONArray(key);
  }

  default void setToProperty(String key, String value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  default void setToProperty(String key, int value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  default void setToProperty(String key, long value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  default void setToProperty(String key, JSONObject value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  default void setToProperty(String key, JSONArray value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  default void putNewArrayToProperty(String key) {
    getPropertyStore().put(key, new ArrayList<>());
    updatePropertyStore();
  }

  default void putToArrayOfProperty(String key, String value) {
    JSONArray array = null;
    boolean isContain = false;
    try {
      array = getPropertyStore().getJSONArray(key);
      isContain = array.toList().contains(value);
    } catch (Exception e) {}
    if (array == null) {
      getPropertyStore().put(key, new ArrayList<>());
      array = getPropertyStore().getJSONArray(key);
    }
    if (! isContain) {
      array.put(value);
    }
    updatePropertyStore();
  }

  default void removeFromArrayOfProperty(String key, String value) {
    try {
      JSONArray array = getPropertyStore().getJSONArray(key);
      int index = array.toList().indexOf(value);
      if (index >= 0) {
        array.remove(index);
      }
    } catch (Exception e) {}
    updatePropertyStore();
  }
}
