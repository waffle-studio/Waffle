package jp.tkms.waffle.data;

import jnr.ffi.annotations.In;
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
import java.util.HashMap;
import java.util.UUID;

abstract public class Data {
  public static final String KEY_ROWID = "rowid";
  public static final String KEY_ID = "id";
  public static final String KEY_NAME = "name";
  public static final String KEY_UUID = "uuid";
  public static final String KEY_DIRECTORY = "directory";
  public static final String KEY_CLASS = "class";

  private static final Object databaseLocker = new Object();

  protected UUID id = null;
  protected String shortId = null;
  protected String name;

  public Data() {}

  public Data(UUID id, String name) {
    this.id = id;
    this.shortId = getShortId(id);
    this.name = name;
  }

  abstract protected String getTableName();

  abstract protected Updater getDatabaseUpdater();

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

  protected void setSystemValue(String name, Object value) {
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, Database.SYSTEM_TABLE,
          Sql.Value.equal( Database.KEY_NAME, name ),
          Sql.Value.equal( Database.KEY_VALUE, value.toString() )
        ).execute();
      }
    });
  }

  protected String getStringFromDB(String key) {
    final String[] result = {null};

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          result[0] = resultSet.getString(key);
        }
      }
    });

    return result[0];
  }

  protected Integer getIntFromDB(String key) {
    final Integer[] result = {null};

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          result[0] = resultSet.getInt(key);
        }
      }
    });

    return result[0];
  }

  protected boolean setToDB(String key, String value) {
    return handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, getTableName(), Sql.Value.equal(key, value)).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
  }

  protected boolean setToDB(String key, int value) {
    return handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, getTableName(), Sql.Value.equal(key, value)).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
  }

  public void setName(String name) {
    if (
      setToDB(KEY_NAME, name)
    ) {
      this.name = name;
    }
  }

  protected Path getPropertyStorePath() {
    return Paths.get("waffle.json");
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
          e.printStackTrace();
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
    return getPropertyStore().getJSONArray(key);
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

  private static void initializeMainDatabase() {
    synchronized (databaseLocker) {
      try (Database db = Database.getDatabase(null)) {
        new Updater() {
          @Override
          String tableName() {
            return KEY_UUID;
          }

          @Override
          ArrayList<UpdateTask> updateTasks() {
            return new ArrayList<Updater.UpdateTask>(Arrays.asList(
              new UpdateTask() {
                @Override
                void task(Database db) throws SQLException {
                  new Sql.Create(db, tableName(), KEY_ID, KEY_NAME, KEY_CLASS, KEY_DIRECTORY).execute();
                }
              }
            ));
          }
        }.update(db);
        db.commit();
      } catch (SQLException e) {
      }
    }
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

  protected Database getDatabase() {
    return Database.getDatabase(null);
  }

  protected static boolean handleDatabase(Data base, Handler handler) {
    boolean isSuccess = true;

    initializeMainDatabase();

    synchronized (databaseLocker) {
      try (Database db = (base == null ? Database.getDatabase(null) : base.getDatabase())) {
        Updater updater = (base == null ? null : base.getDatabaseUpdater());
        if (updater != null) {
          updater.update(db);
        } else {
          db.getVersion(null);
        }
        handler.handling(db);
        db.commit();
      } catch (SQLException e) {
        isSuccess = false;
        e.printStackTrace();
      }
    }

    return isSuccess;
  }

  abstract static class Handler {
    abstract void handling(Database db) throws SQLException;
  }

  public abstract static class Updater {
    public abstract class UpdateTask {
      abstract void task(Database db) throws SQLException;
    }

    abstract String tableName();

    abstract ArrayList<UpdateTask> updateTasks();

    void update(Database db) {
      try {
        int version = db.getVersion(tableName());

        for (; version < updateTasks().size(); version++) {
          updateTasks().get(version).task(db);
        }

        db.setVersion(tableName(), version);
        db.commit();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
