package jp.tkms.waffle.data.web;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.Database;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.StringFileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class UserSession extends Data {
  static final String TABLE_NAME = "user_session";
  private static final String KEY_TIMESTAMP_CREATE = "timestamp_create";

  private static final Object objectLocker = new Object();
  private static String waffleId = null;
  public static String getWaffleId() {
    if (waffleId == null) {
      synchronized (objectLocker) {
        if (Files.exists(Constants.UUID_FILE)) {
          waffleId = StringFileUtil.read(Constants.UUID_FILE).trim();
        } else {
          waffleId = UUID.randomUUID().toString();
          StringFileUtil.write(Constants.UUID_FILE, waffleId.toString());
        }
      }
    }
    return waffleId;
  }

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
      protected void handling(Database db) throws SQLException {
        ResultSet resultSet
          = new Sql.Select(db, TABLE_NAME, KEY_ID).where(Sql.Value.equal(KEY_NAME, sessionId)).executeQuery();
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
      protected void handling(Database db) throws SQLException {
        ResultSet resultSet
          = new Sql.Select(db, TABLE_NAME, KEY_ID).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
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
      protected void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID).executeQuery();
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
      protected void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME, Sql.Value.equal(KEY_ID, session.getId()), Sql.Value.equal(KEY_NAME, session.getName())).execute();
      }
    });

    return session;

  }

  public static void delete(String id) {
    handleDatabase(new UserSession(), new Handler() {
      @Override
      protected void handling(Database db) throws SQLException {
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
      protected String tableName() {
        return TABLE_NAME;
      }

      @Override
      protected ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            protected void task(Database db) throws SQLException {
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
