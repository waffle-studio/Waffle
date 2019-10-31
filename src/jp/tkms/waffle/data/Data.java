package jp.tkms.waffle.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

  abstract protected DatabaseUpdater getMainDatabaseUpdater();

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public static String getUnifiedName(UUID id, String name) {
    return name + '_' + getShortId(id);
  }

  public String getUnifiedName() {
    return name + '_' + getShortId(id);
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

  protected String getFromDB(String key) {
    String result = null;
    try {
      Database db = getMainDB(getMainDatabaseUpdater());
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

  protected static Database getMainDB(DatabaseUpdater updater) {
    Database db = Database.getMainDB();
    if (updater != null) {
      updater.update(db);
    }
    return db;
  }
}
