package jp.tkms.waffle.data;

import jnr.ffi.annotations.In;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Registry extends ProjectData {
  protected static final String TABLE_NAME = "registry";
  private static final String KEY_VALUE = "value";

  enum Type {String, Integer, Double, Boolean};

  public Registry(Project project) {
    super(project, null, null);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ArrayList<KeyValue> getList(Project project) {
    ArrayList<KeyValue> keyValueList = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          keyValueList.add(
            new KeyValue(resultSet.getString("name"), resultSet.getString("name"))
          );
        }
      }
    });

    return keyValueList;
  }

  public ArrayList<KeyValue> getList() {
    return getList(getProject());
  }

  static Object get(Project project, String key, Type type, Object defaultValue) {
    final Object[] result = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select " + KEY_VALUE + " from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          if (Type.Integer.equals(type)) {
            result[0] = resultSet.getInt(KEY_VALUE);
          } else if (Type.Double.equals(type)) {
            result[0] = resultSet.getDouble(KEY_VALUE);
          } else if (Type.Boolean.equals(type)) {
            result[0] = resultSet.getBoolean(KEY_VALUE);
          } else {
            result[0] = resultSet.getString(KEY_VALUE);
          }
          break;
        }
      }
    });

    return (result[0] == null ? defaultValue : result[0]);
  }

  static void set(Project project, String key, Object value) {
    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("delete from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, key);
        statement.execute();
        statement = db.preparedStatement("inset into " + TABLE_NAME + "(name,value) values(?,?);");
        statement.setString(1, key);
        if (value instanceof Integer) {
          statement.setInt(2, (Integer)value);
        } else if (value instanceof Double || value instanceof Float) {
          statement.setDouble(2, (Double)value);
        } else if (value instanceof Boolean) {
          statement.setBoolean(2, (Boolean)value);
        } else {
          statement.setString(2, value.toString());
        }
        statement.execute();
      }
    });
  }

  public void set(String key, Object value) {
    set(getProject(), key, value);
  }

  public static String getString(Project project, String key, String defaultValue) {
    return (String)get(project, key, Type.String, defaultValue);
  }

  public String getString(String key, String defaultValue) {
    return getString(getProject(), key, defaultValue);
  }

  public static Integer getInteger(Project project, String key, String defaultValue) {
    return (Integer) get(project, key, Type.Integer, defaultValue);
  }

  public Integer getInteger(String key, String defaultValue) {
    return getInteger(getProject(), key, defaultValue);
  }

  public static Double getDouble(Project project, String key, String defaultValue) {
    return (Double) get(project, key, Type.Double, defaultValue);
  }

  public Double getDouble(String key, String defaultValue) {
    return getDouble(getProject(), key, defaultValue);
  }

  public static Boolean getBoolean(Project project, String key, String defaultValue) {
    return (Boolean) get(project, key, Type.Boolean, defaultValue);
  }

  public Boolean getBoolean(String key, String defaultValue) {
    return getBoolean(getProject(), key, defaultValue);
  }

  @Override
  protected Updater getMainUpdater() {
    return null;
  }

  @Override
  protected Updater getWorkUpdater() {
    return workUpdater;
  }

  private static Updater workUpdater = new Updater() {
    @Override
    String tableName() {
      return TABLE_NAME;
    }

    @Override
    ArrayList<UpdateTask> updateTasks() {
      return new ArrayList<UpdateTask>(Arrays.asList(
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            db.execute("create table " + TABLE_NAME + "(" +
              KEY_NAME + "," + KEY_VALUE +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
