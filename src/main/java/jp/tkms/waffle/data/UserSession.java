package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class UserSession extends Data {
  static final String TABLE_NAME = "user_session";
  public static final String KEY_SESSION_ID = "session_id";
  private static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  public UserSession(String sessionId) {
    super(UUID.randomUUID(), sessionId);
  }

  public UserSession() { }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }


  public static boolean isContains(String sessionId) {
    final boolean[] isContains = {false};

    handleDatabase(new UserSession(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Select(db, TABLE_NAME, KEY_ID).where(Sql.Value.equalP(KEY_NAME)).toPreparedStatement();
        statement.setString(1, sessionId);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          isContains[0] = true;
        }
      }
    });

    return isContains[0];
  }

  public static UserSession getInstance(String id) {
    final UserSession[] browser = {null};

    handleDatabase(new UserSession(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Select(db, TABLE_NAME, KEY_ID).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          browser[0] = new UserSession(
            resultSet.getString(KEY_ID)
          );
        }
      }
    });

    return browser[0];
  }

  public static ArrayList<UserSession> getList() {
    ArrayList<UserSession> list = new ArrayList<>();

    handleDatabase(null, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID).toPreparedStatement().executeQuery();
        while (resultSet.next()) {
          list.add(new UserSession(
            resultSet.getString(KEY_ID)
          ));
        }
      }
    });

    return list;
  }

  public static UserSession create() {
    UserSession session = new UserSession(UUID.randomUUID().toString() + '-' + UUID.randomUUID().toString());

    handleDatabase(new UserSession(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Insert(db, TABLE_NAME, KEY_ID, KEY_NAME).toPreparedStatement();
        statement.setString(1, session.getId());
        statement.setString(2, session.getName());
        statement.execute();
      }
    });

    return session;

  }

  public static void delete(String id) {
    handleDatabase(new UserSession(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("delete from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        statement.execute();
      }
    });
  }

  public String getSessionId() {
    return getName();
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
              new Sql.Create(db, TABLE_NAME,
                KEY_ID, KEY_NAME,
                Sql.Create.timestamp(KEY_TIMESTAMP_CREATE)
                ).execute();
            }
          }
        ));
      }
    };
  }

}
