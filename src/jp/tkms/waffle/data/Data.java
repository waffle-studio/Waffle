package jp.tkms.waffle.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

abstract public class Data {
  protected static final String KEY_ID = "id";
  protected static final String KEY_NAME = "name";

  protected UUID id = null;
  protected String shortId;
  protected String name;

  public Data(UUID id, String name) {
    this.id = id;
    this.shortId = getShortId(id);
    this.name = name;
  }

  abstract protected String getTableName();

  abstract protected Updater getMainUpdater();

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public static String getShortName(String name) {
    String replacedName = name.replaceAll("[^0-9a-zA-Z_\\-]", "");
    return replacedName.substring(0, (replacedName.length() < 8 ? replacedName.length() : 8));
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

  public String getId() {
    return id.toString();
  }

  public String getShortId() {
    return shortId;
  }

  public String getShortName() {
    return getShortName(name);
  }

  protected String getFromDB(String key) {
    String result = null;
    try {
      Database db = getMainDB(getMainUpdater());
      PreparedStatement statement
        = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
      statement.setString(1, getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        result = resultSet.getString(key);
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static Database getMainDB(Updater updater) {
    Database db = Database.getMainDB();
    if (updater != null) {
      updater.update(db);
    }
    return db;
  }

  synchronized protected static boolean handleMainDB(Updater updater, Handler handler) {
    boolean isSuccess = true;

    try(Database db = getMainDB(updater)) {
      handler.handling(db);
      db.commit();
    } catch (SQLException e) {
      isSuccess = false;
      e.printStackTrace();
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
