package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.LogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.Sql;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static jp.tkms.waffle.data.AbstractRun.KEY_TIMESTAMP_CREATE;

public class Log extends Data {
  protected static final String TABLE_NAME = "log";
  private static final String KEY_LEVEL = "level";
  private static final String KEY_MESSAGE = "message";

  private Level level = null;
  private String message = null;


  public Log() {
    super();
  }

  public Log(UUID id, String name, Level level, String mmessage) {
    super(id, name);
    this.level = level;
    this.message = mmessage;
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Log getInstance(String id) {
    final Log[] log = {null};

    handleDatabase(new Log(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_LEVEL, KEY_MESSAGE).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          log[0] = new Log(
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            Level.valueOf(resultSet.getInt(KEY_LEVEL)),
            resultSet.getString(KEY_MESSAGE)
          );
        }
      }
    });

    return log[0];
  }

  public static ArrayList<Log> getList(int from, int limit) {
    ArrayList<Log> logs = new ArrayList<>();

    handleDatabase(new Log(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        Sql.Select select = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_LEVEL, KEY_MESSAGE);
        if (from >= 0) {
          select.where(Sql.Value.greeterThan(KEY_ROWID, from));
        }
        select.limit(limit);
        ResultSet resultSet = select.executeQuery();
        while (resultSet.next()) {
          logs.add(new Log(
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            Level.valueOf(resultSet.getInt(KEY_LEVEL)),
            resultSet.getString(KEY_MESSAGE)
          ));
        }
      }
    });

    return logs;
  }

  public static Log create(Level level, String message) {
    System.err.println('[' + level.name() + "] " + message);

    Log log = new Log(UUID.randomUUID(), "", level, message);
    BrowserMessage.addMessage("error('" + message + "');");

    handleDatabase(new Log(), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          new Sql.Insert(db, TABLE_NAME,
            Sql.Value.equal(KEY_ID, log.getId()),
            Sql.Value.equal(KEY_LEVEL, level.ordinal()),
            Sql.Value.equal(KEY_MESSAGE, message)
          ).execute();
        }
      });

    return log;
  }

  public static Log create(LogMessage log) {
    String message = log.getMessage().replaceAll("\"", "").replaceAll("'", "").replaceAll("\n", "");
    if (log instanceof InfoLogMessage) {
      return create(Level.Info, log.getMessage());
    } else if (log instanceof WarnLogMessage) {
      return create(Level.Warn, log.getMessage());
    } else if (log instanceof ErrorLogMessage) {
      return create(Level.Error, log.getMessage());
    }
    return create(Level.Debug, log.getMessage());
  }

  public Path getDirectoryPath() {
    return getWaffleDirectoryPath().resolve(Constants.LOG);
  }

  @Override
  protected Database getDatabase() {
    return Database.getDatabase(Paths.get(getDirectoryPath() + File.separator + Constants.LOG_DB_NAME));
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
                KEY_ID,
                Sql.Create.withDefault(KEY_NAME, "''"),
                Sql.Create.withDefault(KEY_LEVEL, "0"),
                Sql.Create.withDefault(KEY_MESSAGE, "''"),
                Sql.Create.timestamp(KEY_TIMESTAMP_CREATE)).execute();
            }
          }
        ));
      }
    };
  }

  public Level getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public String getTimestamp() {
    return getStringFromDB(KEY_TIMESTAMP_CREATE);
  }

  public enum Level {
    Debug, Info, Warn, Error;
    public static Level valueOf(int i) { return values()[i]; }
  }
}
