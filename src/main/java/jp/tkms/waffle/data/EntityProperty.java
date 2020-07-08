package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.ErrorLogMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public interface EntityProperty {
  Path getPropertyStorePath();

  JSONObject[] propertyStore = {null};

  default JSONObject getPropertyStore() {
    if (propertyStore[0] == null) {
      Path storePath = getPropertyStorePath();
      String json = "{}";
      if (Files.exists(storePath)) {
        try {
          json = new String(Files.readAllBytes(storePath));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      propertyStore[0] = new JSONObject(json);
    }
    return propertyStore[0];
  }

  default void updatePropertyStore() {
    if (propertyStore != null) {
      Path directoryPath = getPropertyStorePath().getParent();
      if (! Files.exists(directoryPath)) {
        try {
          Files.createDirectories(directoryPath);
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }

      try {
        FileWriter filewriter = new FileWriter(getPropertyStorePath().toFile());
        filewriter.write(propertyStore[0].toString(2));
        filewriter.close();
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
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
    return getArrayFromProperty(key, true);
  }

  default JSONArray getArrayFromProperty(String key, boolean returnNull) {
    JSONArray jsonArray = null;
    try {
      jsonArray = getPropertyStore().getJSONArray(key);
    } catch (Exception e) {}
    if ((! returnNull) && jsonArray == null) {
      jsonArray = new JSONArray();
      setToProperty(key, jsonArray);
    }
    return jsonArray;
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
