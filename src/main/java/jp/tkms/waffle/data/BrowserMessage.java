package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class BrowserMessage extends Data {
  private static final String TABLE_NAME = "browser_message";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_ROWID = "rowid";
  private static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  private int rowId;
  private String message = null;

  public BrowserMessage(UUID id, int rowId, String message) {
    super(id, "");
    this.rowId = rowId;
    this.message = message;
  }

  public BrowserMessage() { }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ArrayList<BrowserMessage> getList(String currentRowId) {
    ArrayList<BrowserMessage> list = new ArrayList<>();

    handleDatabase(new BrowserMessage(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createSelect(TABLE_NAME,
          KEY_ID, KEY_MESSAGE, KEY_ROWID
        ).where(Sql.Value.greeterThanP(KEY_ROWID)).toPreparedStatement();
        statement.setString(1, currentRowId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new BrowserMessage(
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getInt(KEY_ROWID),
            resultSet.getString(KEY_MESSAGE)
          ));
        }
      }
    });

    return list;
  }

  public static void removeExpired(Database db) throws SQLException {
    db.execute("delete from " + TABLE_NAME
      + " where " + KEY_TIMESTAMP_CREATE + " < datetime('now', '-5 seconds');");
    db.commit();
  }

  public static void addMessage(String message) {
    handleDatabase(new BrowserMessage(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        removeExpired(db);

        PreparedStatement statement = new Sql.Insert(db, TABLE_NAME, KEY_ID, KEY_MESSAGE).toPreparedStatement();
        statement.setString(1, UUID.randomUUID().toString());
        statement.setString(2, "try{" + message + "}catch(e){}");
        statement.execute();
      }
    });
  }

  public static int getCurrentRowId() {
    final int[] currentRowId = {-1};
    handleDatabase(new BrowserMessage(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, "max(rowid) as cid").toPreparedStatement().executeQuery();
        while (resultSet.next()) {
            currentRowId[0] = resultSet.getInt("cid");
        }
      }
    });
    return currentRowId[0];
  }

  public int getRowId() {
    return rowId;
  }

  public String getMessage() {
    return message;
  }

  public void remove() {
    handleDatabase(new BrowserMessage(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("delete from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        statement.execute();
      }
    });
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
              db.execute("create table " + TABLE_NAME + "(id," +
                KEY_MESSAGE+ "," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

}
