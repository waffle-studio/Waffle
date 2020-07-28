package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.component.updater.LogUpdater;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.LogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.Sql;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Log {
  protected static final String TABLE_NAME = "log";
  private static final String KEY_ID = "id";
  private static final String KEY_LEVEL = "level";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_TIMESTAMP = "timestamp";

  private static ExecutorService loggerThread = Executors.newSingleThreadExecutor();
  private static final Object objectLocker = new Object();
  private static Long nextId = 0L;

  private long timestamp = 0;
  private long id = -1;
  private Level level = null;
  private String message = null;

  private Log(long id, long timestamp, Level level, String message) {
    this.id = id;
    this.timestamp = timestamp;
    this.level = level;
    this.message = message;
  }

  private Log(Level level, String message) {
    this(getNextId(), (System.currentTimeMillis() / 1000L), level, message);
  }

  public static long getNextId() {
    synchronized (nextId) {
      return nextId++;
    }
  }

  public static ArrayList<Log> getDescList(int from, int limit) {
    ArrayList<Log> logs = new ArrayList<>();

    synchronized (objectLocker) {
      Database db = getDatabase();
      try {
        tryCreateTable(db);
        Sql.Select select = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_LEVEL, KEY_MESSAGE, KEY_TIMESTAMP);
        if (from >= 0) {
          select.where(Sql.Value.lessThan(KEY_ID, from));
        }
        select.orderBy(KEY_ID, true);
        select.limit(limit);
        ResultSet resultSet = select.executeQuery();
        while (resultSet.next()) {
          logs.add(new Log(
            resultSet.getLong(KEY_ID),
            resultSet.getLong(KEY_TIMESTAMP),
            Level.valueOf(resultSet.getInt(KEY_LEVEL)),
            resultSet.getString(KEY_MESSAGE)
          ));
        }
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      } finally {
        try {
          db.close();
        } catch (SQLException e) {
          ErrorLogMessage.issue(e);
        }
      }
    }

    return logs;
  }

  public static Log create(Level level, String message) {
    System.err.println('[' + level.name() + "] " + message);

    Log log = new Log(level, message);

    loggerThread.submit(() -> {
      synchronized (objectLocker) {
        Database db = getDatabase();
        try {
          tryCreateTable(db);
          new Sql.Insert(db, TABLE_NAME,
            Sql.Value.equal(KEY_LEVEL, log.level.ordinal()),
            Sql.Value.equal(KEY_MESSAGE, log.message),
            Sql.Value.equal(KEY_TIMESTAMP, log.timestamp),
            Sql.Value.equal(KEY_ID, log.id)
          ).execute();
          db.commit();
        } catch (SQLException e) {
          ErrorLogMessage.issue(e);
        } finally {
          try {
            db.close();
          } catch (SQLException e) {
            ErrorLogMessage.issue(e);
          }
        }
      }
    });

    new LogUpdater(log);

    return log;
  }

  public static Log create(LogMessage log) {
    if (log instanceof InfoLogMessage) {
      return create(Level.Info, log.getMessage());
    } else if (log instanceof WarnLogMessage) {
      return create(Level.Warn, log.getMessage());
    } else if (log instanceof ErrorLogMessage) {
      return create(Level.Error, log.getMessage());
    }
    return create(Level.Debug, log.getMessage());
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.LOG);
  }

  private static Database getDatabase() {
    return Database.getDatabase(getDirectoryPath().resolve(Constants.LOG_DB_NAME));
  }

  private static void tryCreateTable(Database db) throws SQLException {
    new Sql.Create(db, TABLE_NAME,
      Sql.Create.withDefault(KEY_ID, "-1"),
      Sql.Create.withDefault(KEY_LEVEL, "0"),
      Sql.Create.withDefault(KEY_MESSAGE, "''"),
      Sql.Create.withDefault(KEY_TIMESTAMP, "0")).execute();
  }

  public Level getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public String getTimestamp() {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp),
      TimeZone.getDefault().toZoneId()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  public long getId() {
    return id;
  }

  public enum Level {
    Debug, Info, Warn, Error;
    public static Level valueOf(int i) { return values()[i]; }
  }
}
