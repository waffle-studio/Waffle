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

public class BrowserMessage extends Data {
  private static final String TABLE_NAME = "browser_message";
  private static final String KEY_BROWSER_ID = "browser";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  private String browserId = null;
  private String message = null;

  public BrowserMessage(UUID id, String browserId, String message) {
    super(id, "");
    this.browserId = browserId;
    this.message = message;
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ArrayList<BrowserMessage> getList(String browserId) {
    ArrayList<BrowserMessage> list = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);

      PreparedStatement statement = db.createSelect(TABLE_NAME,
        KEY_ID, KEY_BROWSER_ID, KEY_MESSAGE
      ).where(Sql.Value.equalP(KEY_BROWSER_ID)).preparedStatement();
      statement.setString(1, browserId);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        list.add(new BrowserMessage(
          UUID.fromString(resultSet.getString(KEY_ID)),
          resultSet.getString(KEY_BROWSER_ID),
          resultSet.getString(KEY_MESSAGE)
        ));
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static void removeExpired(Database db) throws SQLException {
    db.execute("delete from " + TABLE_NAME
      + " where " + KEY_TIMESTAMP_CREATE + " < datetime('now', '-5 seconds');");
    db.commit();
  }

  public static void addMessage(String message) {

    try {
      Database db = getMainDB(mainDatabaseUpdater);
      Browser.removeExpired(db);
      removeExpired(db);

      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,"
        + KEY_BROWSER_ID + ","
        + KEY_MESSAGE
        + ") values(?,(select id from " + Browser.TABLE_NAME + "),?);");
      statement.setString(1, UUID.randomUUID().toString());
      statement.setString(2, message);
      statement.execute();
      db.commit();
      db.close();

      //Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return;
  }

  public String getMessage() {
    if (message == null) {
      message = getFromDB(KEY_MESSAGE);
    }
    return message;
  }

  public void remove() {
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("delete from " + getTableName() + " where id=?;");
      statement.setString(1, getId());
      statement.execute();
      db.commit();
      db.close();
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
                KEY_BROWSER_ID+ "," +
                KEY_MESSAGE+ "," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
}
