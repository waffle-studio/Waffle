package jp.tkms.waffle.data.log;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.web.updater.LogUpdater;
import jp.tkms.waffle.data.util.Database;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Log {
  protected static final String TABLE_NAME = "log";
  private static final String KEY_ID = "id";
  private static final String KEY_LEVEL = "level";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_TIMESTAMP = "timestamp";
  private static final int MILLI_SEC_OF_24HOUR = 1000 * 60 * 60 * 24;

  private static long currentDatabaseCreatedTimestamp;
  private static String currentDatabaseName = newDatabaseName();
  private static ExecutorService loggerThread = Executors.newSingleThreadExecutor();
  private static final Object objectLocker = new Object();
  private static AtomicLong nextId = new AtomicLong(0);

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
    this(nextId.getAndIncrement(), (System.currentTimeMillis() / 1000L), level, message);
  }

  public static String getCurrentDatabaseName() {
    return currentDatabaseName;
  }

  private static String newDatabaseName() {
    currentDatabaseCreatedTimestamp = System.currentTimeMillis();
    return currentDatabaseName = "LOG-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".db";
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

  private static void logRotate() {
    Database db = getDatabase();
    String currentDatabaseName = getCurrentDatabaseName();
    String nextDatabaseName = newDatabaseName();
    Log log = new Log(Level.RotateTo, nextDatabaseName);
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

    nextId.set(0);
    db = getDatabase();
    log = new Log(Level.RotateFrom, currentDatabaseName);
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

  public static Log create(Level level, String message) {
    Log log = new Log(level, message);

    loggerThread.submit(() -> {
      System.err.println('[' + level.name() + "] " + message);
      synchronized (objectLocker) {
        if (System.currentTimeMillis() - currentDatabaseCreatedTimestamp > MILLI_SEC_OF_24HOUR) {
          logRotate();
        }

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
      new LogUpdater(log);
    });

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

  public static Path getDatabasePath() {
    return getDirectoryPath().resolve(getCurrentDatabaseName());
  }

  private static Database getDatabase() {
    return Database.getDatabase(getDatabasePath());
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
    Debug, Info, Warn, Error, RotateTo, RotateFrom;
    public static Level valueOf(int i) { return values()[i]; }
  }

  public static void close() {
    loggerThread.shutdown();
    try {
      loggerThread.awaitTermination(7, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
