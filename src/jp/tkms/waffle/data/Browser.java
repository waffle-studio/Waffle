package jp.tkms.waffle.data;

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
    final Browser[] browser = {null};

    handleMainDB(mainUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select id from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          browser[0] = new Browser(
            UUID.fromString(resultSet.getString("id"))
          );
        }
      }
    });

    return browser[0];
  }

  public static String getNewId() {
    Browser browser = new Browser(UUID.randomUUID());

    handleMainDB(mainUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("insert into " + TABLE_NAME + "(id) values(?);");
        statement.setString(1, browser.getId());
        statement.execute();
      }
    });

    return browser.getId();
  }

  public static ArrayList<Browser> getList() {
    ArrayList<Browser> list = new ArrayList<>();

    handleMainDB(mainUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          list.add(new Browser(
            UUID.fromString(resultSet.getString("id"))
          ));
        }
      }
    });

    return list;
  }

  public static void update(String id) {
    handleMainDB(mainUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("delete from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        statement.execute();

        statement = db.preparedStatement("insert into " + TABLE_NAME + "(" + KEY_ID + ") values(?);");
        statement.setString(1, id);
        statement.execute();
      }
    });
  }

  public static void removeExpired(Database db) throws SQLException {
    db.execute("delete from " + TABLE_NAME
      + " where " + KEY_TIMESTAMP_CREATE + " < datetime('now', '-5 seconds');");
    db.commit();
  }

  public static void updateDB() {
    handleMainDB(mainUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
      }
    });
  }

  @Override
  protected Updater getMainUpdater() {
    return mainUpdater;
  }

  private static Updater mainUpdater = new Updater() {
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
