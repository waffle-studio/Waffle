package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

abstract public class PropertyFileData {
  public static final String KEY_ID = "id";
  public static final String KEY_NAME = "name";
  public static final String KEY_UUID = "uuid";
  public static final String KEY_DIRECTORY = "directory";
  public static final String KEY_CLASS = "class";

  protected UUID id = null;
  protected String shortId = null;
  protected String name;

  public PropertyFileData() {}

  public PropertyFileData(UUID id, String name) {
    this.id = id;
    this.shortId = getShortId(id);
    this.name = name;
  }

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public static String getShortName(String name) {
    String replacedName = name.replaceAll("[^0-9a-zA-Z_\\-]", "");
    return replacedName.substring(0, (replacedName.length() < 8 ? replacedName.length() : 8));
  }

  public static Path getWaffleDirectoryPath() {
    return Constants.WORK_DIR;
  }

  public static String getUnifiedName(UUID id, String name) {
    return getShortName(name) + '_' + getShortId(id);
  }

  public String getUnifiedName() {
    return getShortName() + '_' + getShortId();
  }

  public boolean isValid() {
    return id != null;
  }

  public String getName() {
    return name;
  }

  public UUID getUuid() {
    return id;
  }

  public String getId() {
    if (id == null) {
      getUuid();
    }
    return id.toString();
  }

  public String getShortId() {
    if (shortId == null) {
      shortId = getShortId(getUuid());
    }
    return shortId;
  }

  public String getShortName() {
    return getShortName(name);
  }

  public void setName(String name) {
    setToProperty(KEY_NAME, name);
    this.name = name;
  }

  protected Path getPropertyStorePath() {
    return Paths.get("waffle.json").toAbsolutePath();
  }

  private JSONObject propertyStore = null;
  private JSONObject getPropertyStore() {
    if (propertyStore == null) {
      Path storePath = getPropertyStorePath();
      String json = "{}";
      if (Files.exists(storePath)) {
        try {
          json = new String(Files.readAllBytes(storePath));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      propertyStore = new JSONObject(json);
    }
    return propertyStore;
  }

  private void updatePropertyStore() {
    if (propertyStore != null) {
      Path directoryPath = getPropertyStorePath().getParent();
      if (! Files.exists(directoryPath)) {
        try {
          Files.createDirectories(directoryPath);
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }

      Path storePath = getPropertyStorePath();
      try {
        FileWriter filewriter = new FileWriter(storePath.toFile());
        filewriter.write(propertyStore.toString(2));
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  protected String getStringFromProperty(String key) {
    try {
      return getPropertyStore().getString(key);
    } catch (Exception e) {}
    return null;
  }

  protected String getStringFromProperty(String key, String defaultValue) {
    String value = getStringFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  protected Integer getIntFromProperty(String key) {
    try {
      return getPropertyStore().getInt(key);
    } catch (Exception e) {}
    return null;
  }

  protected Integer getIntFromProperty(String key, Integer defaultValue) {
    Integer value = getIntFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  protected Long getLongFromProperty(String key) {
    try {
      return getPropertyStore().getLong(key);
    } catch (Exception e) {}
    return null;
  }

  protected JSONObject getJSONObjectFromProperty(String key) {
    try {
      return getPropertyStore().getJSONObject(key);
    } catch (Exception e) {}
    return null;
  }

  protected JSONObject getJSONObjectFromProperty(String key, JSONObject defaultValue) {
    JSONObject value = getJSONObjectFromProperty(key);
    if (value == null) {
      value = defaultValue;
      if (value != null) {
        setToProperty(key, defaultValue);
      }
    }
    return value;
  }

  protected JSONArray getArrayFromProperty(String key) {
    return getArrayFromProperty(key, true);
  }

  protected JSONArray getArrayFromProperty(String key, boolean returnNull) {
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

  protected void setToProperty(String key, String value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  protected void setToProperty(String key, int value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  protected void setToProperty(String key, long value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  protected void setToProperty(String key, JSONObject value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  protected void setToProperty(String key, JSONArray value) {
    getPropertyStore().put(key, value);
    updatePropertyStore();
  }

  protected void putNewArrayToProperty(String key) {
    getPropertyStore().put(key, new ArrayList<>());
    updatePropertyStore();
  }

  protected void putToArrayOfProperty(String key, String value) {
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

  protected void removeFromArrayOfProperty(String key, String value) {
    try {
      JSONArray array = getPropertyStore().getJSONArray(key);
      int index = array.toList().indexOf(value);
      if (index >= 0) {
        array.remove(index);
      }
    } catch (Exception e) {}
    updatePropertyStore();
  }

  public static void initializeWorkDirectory() {
    createDirectories(Constants.WORK_DIR);
    createDirectories(Project.getBaseDirectoryPath());
    createDirectories(ConductorTemplate.getBaseDirectoryPath());
    createDirectories(ListenerTemplate.getBaseDirectoryPath());
    createDirectories(Host.getBaseDirectoryPath());
  }

  protected static void createDirectories(Path path) {
    if (!Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public void initialize() {
    initializeWorkDirectory();
  }
}
