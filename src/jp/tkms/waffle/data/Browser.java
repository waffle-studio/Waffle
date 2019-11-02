package jp.tkms.waffle.data;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Browser extends Data {
  static final String TABLE_NAME = "browser";
  private static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  public Browser(UUID id) {
    super(id, "");
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Browser getInstance(String id) {
    Browser browser = null;
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("select id from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        browser = new Browser(
          UUID.fromString(resultSet.getString("id"))
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return browser;
  }

  public static String getNewId() {
    Browser browser = new Browser(UUID.randomUUID());
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id) values(?);");
      statement.setString(1, browser.getId());
      statement.execute();
      db.commit();
      db.close();

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return browser.getId();
  }

  public static ArrayList<Browser> getList() {
    ArrayList<Browser> list = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        list.add(new Browser(
          UUID.fromString(resultSet.getString("id"))
        ));
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static void update(String id) {
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("delete from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      statement.execute();

      statement = db.preparedStatement("insert into " + TABLE_NAME + "(" + KEY_ID + ") values(?);");
      statement.setString(1, id);
      statement.execute();

      db.commit();
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void removeExpired(Database db) throws SQLException {
    db.execute("delete from " + TABLE_NAME
      + " where " + KEY_TIMESTAMP_CREATE + " < datetime('now', '-5 seconds');");
    db.commit();
  }

  public static void updateDB() {
    try {
      getMainDB(mainDatabaseUpdater).close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return mainDatabaseUpdater;
  }

  private static DatabaseUpdater mainDatabaseUpdater = new DatabaseUpdater() {
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
              db.execute("create table " + TABLE_NAME + "(id," +
                "timestamp_create timestamp default (DATETIME('now'))" +
                ");");
            }
          }
        ));
      }
    };
}
