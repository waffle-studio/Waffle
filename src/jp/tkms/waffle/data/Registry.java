package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.KeyValue;
import jp.tkms.waffle.data.util.ValueType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Registry extends ProjectData implements Map<Object, Object> {
  protected static final String TABLE_NAME = "registry";
  private static final String KEY_VALUE = "value";

  public Registry(Project project) {
    super(project, UUID.randomUUID(), "");
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ArrayList<KeyValue> getList(Project project) {
    ArrayList<KeyValue> keyValueList = new ArrayList<>();

    handleDatabase(new Registry(project), new Handler() {
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

  static Object get(Project project, String key, ValueType type, Object defaultValue) {
    final Object[] result = {null};

    handleDatabase(new Registry(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select " + KEY_VALUE + " from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, key);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          if (ValueType.Integer.equals(type)) {
            result[0] = resultSet.getInt(KEY_VALUE);
          } else if (ValueType.Double.equals(type)) {
            result[0] = resultSet.getDouble(KEY_VALUE);
          } else if (ValueType.Boolean.equals(type)) {
            result[0] = resultSet.getBoolean(KEY_VALUE);
          } else if (ValueType.String.equals(type)) {
            result[0] = resultSet.getString(KEY_VALUE);
          } else {
            result[0] = resultSet.getObject(KEY_VALUE);
          }
          break;
        }
      }
    });

    return (result[0] == null ? defaultValue : result[0]);
  }

  public Object get(String key) {
    return get(getProject(), key, null, null);
  }

  public Object get(String key, Object defaultValue) {
    return get(getProject(), key, null, defaultValue);
  }

  static void set(Project project, String key, Object value) {
    handleDatabase(new Registry(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("delete from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, key);
        statement.execute();
        if (value != null) {
          statement = db.preparedStatement("insert into " + TABLE_NAME + "(name,value) values(?,?);");
          statement.setString(1, key);
          if (value instanceof Integer) {
            statement.setInt(2, (Integer) value);
          } else if (value instanceof Double || value instanceof Float) {
            statement.setDouble(2, (Double) value);
          } else if (value instanceof Boolean) {
            statement.setBoolean(2, (Boolean) value);
          } else {
            statement.setString(2, value.toString());
          }
          statement.execute();
        }
      }
    });
  }

  public void set(String key, Object value) {
    set(getProject(), key, value);
  }

  public static String getString(Project project, String key, String defaultValue) {
    return (String)get(project, key, ValueType.String, defaultValue);
  }

  public String getString(String key, String defaultValue) {
    return getString(getProject(), key, defaultValue);
  }

  public static Integer getInteger(Project project, String key, Integer defaultValue) {
    return (Integer) get(project, key, ValueType.Integer, defaultValue);
  }

  public Integer getInteger(String key, Integer defaultValue) {
    return getInteger(getProject(), key, defaultValue);
  }

  public static Double getDouble(Project project, String key, Double defaultValue) {
    return (Double) get(project, key, ValueType.Double, defaultValue);
  }

  public Double getDouble(String key, Double defaultValue) {
    return getDouble(getProject(), key, defaultValue);
  }

  public static Boolean getBoolean(Project project, String key, Boolean defaultValue) {
    return (Boolean) get(project, key, ValueType.Boolean, defaultValue);
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return getBoolean(getProject(), key, defaultValue);
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
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
                KEY_NAME + "," + KEY_VALUE + "," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object o) {
    return false;
  }

  @Override
  public boolean containsValue(Object o) {
    return false;
  }

  @Override
  public Object get(Object o) {
    return get(o.toString());
  }

  @Override
  public Object put(Object o, Object o2) {
    set(o.toString(), o2);
    return o2;
  }

  @Override
  public Object remove(Object o) {
    return null;
  }

  @Override
  public void putAll(Map<?, ?> map) {

  }

  @Override
  public void clear() {

  }

  @Override
  public Set<Object> keySet() {
    return null;
  }

  @Override
  public Collection<Object> values() {
    return null;
  }

  @Override
  public Set<Entry<Object, Object>> entrySet() {
    return null;
  }

}
