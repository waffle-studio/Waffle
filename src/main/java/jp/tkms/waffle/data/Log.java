package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.LogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Log extends Data {
  protected static final String TABLE_NAME = "log";
  private static final String KEY_LEVEL = "level";
  private static final String KEY_MESSAGE = "message";

  private String arguments = null;

  public Log() {
    super();
  }

  public static ArrayList<String> getConductorNameList() {
    return new ArrayList<>(Arrays.asList(
      RubyConductor.class.getCanonicalName(),
      TestConductor.class.getCanonicalName()
    ));
  }

  public Log(UUID id, String name) {
    super(id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Log getInstance(String id) {
    final Log[] conductor = {null};

    handleDatabase(new Log(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new Log(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static Log getInstanceByName(String name) {
    final Log[] conductor = {null};

    handleDatabase(new Log(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new Log(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static Log find(String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(key);
    }
    return getInstanceByName(key);
  }

  public static ArrayList<Log> getList() {
    ArrayList<Log> simulatorList = new ArrayList<>();

    handleDatabase(new Log(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new Log(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return simulatorList;
  }

  public static Log create(Level level, String message) {
    System.err.println('[' + level.name() + "] " + message);

    Log log = new Log(UUID.randomUUID(), "");

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
      BrowserMessage.addMessage("info('" + message + "');");
      return create(Level.INFO, log.getMessage());
    } else if (log instanceof WarnLogMessage) {
      BrowserMessage.addMessage("warn('" + message + "');");
      return create(Level.WARN, log.getMessage());
    } else if (log instanceof ErrorLogMessage) {
      BrowserMessage.addMessage("error('" + message + "');");
      return create(Level.ERROR, log.getMessage());
    }
    return create(Level.DEBUG, log.getMessage());
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
                Sql.Create.timestamp("timestamp_create")).execute();
            }
          }
        ));
      }
    };
  }

  public enum Level {DEBUG, INFO, WARN, ERROR}
}
