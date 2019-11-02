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
  private static final String KEY_BROWSER = "browser";
  private static final String KEY_MESSAGE = "message";

  private Project project = null;
  private Host host = null;
  private Run run = null;
  private String jobId = null;

  public BrowserMessage(UUID id, String name) {
    super(id, "");
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static BrowserMessage getInstance(String id) {
    BrowserMessage job = null;
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        job = new BrowserMessage(
          UUID.fromString(resultSet.getString("id")),
          null
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return job;
  }

  public static ArrayList<BrowserMessage> getList() {
    ArrayList<BrowserMessage> list = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        list.add(new BrowserMessage(
          UUID.fromString(resultSet.getString("id")),
              null
        ));
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static ArrayList<BrowserMessage> getList(Host host) {
    ArrayList<BrowserMessage> list = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("select id from " + TABLE_NAME + " where " + KEY_HOST + "=?;");
      statement.setString(1, host.getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        list.add(new BrowserMessage(
          UUID.fromString(resultSet.getString("id")),
          null
        ));
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static void addRun(Run run) {
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,"
        + KEY_PROJECT + ","
        + KEY_HOST
        + ") values(?,?,?);");
      statement.setString(1, run.getId());
      statement.setString(2, run.getProject().getId());
      statement.setString(3, run.getHost().getId());
      statement.execute();
      db.commit();
      db.close();

      //Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return;
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

  public void setJobId(String jobId) {
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("update " + getTableName() + " set " + KEY_JOB_ID + "=?" + " where id=?;");
      statement.setString(1, jobId);
      statement.setString(2, getId());
      statement.execute();
      db.commit();
      db.close();

      this.jobId = jobId;
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public Path getLocation() {
    Path path = Paths.get( TABLE_NAME + File.separator + name + '_' + shortId );
    return path;
  }

  public Project getProject() {
    if (project == null) {
      project = Project.getInstance(getFromDB(KEY_PROJECT));
    }
    return project;
  }

  public Host getHost() {
    if (host == null) {
      host = Host.getInstance(getFromDB(KEY_HOST));
    }
    return host;
  }

  public Run getRun() {
    if (run == null) {
      run = Run.getInstance(getProject(), getId());
    }
    return run;
  }

  public String getJobId() {
    if (jobId == null) {
      jobId = getFromDB(KEY_JOB_ID);
    }
    return jobId;
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
                KEY_PROJECT + "," +
                KEY_HOST + "," +
                KEY_JOB_ID + " default 0," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
}
