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

public class Job extends Data {
  private static final String TABLE_NAME = "job";
  private static final String KEY_PROJECT = "projrct";
  private static final String KEY_HOST = "host";

  private Project project = null;
  private Host host = null;

  public Job(UUID id, String name) {
    super(id, "");
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Job getInstance(String id) {
    Job host = null;
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        host = new Job(
          UUID.fromString(resultSet.getString("id")),
          null
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return host;
  }

  public static ArrayList<Job> getList() {
    ArrayList<Job> list = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        list.add(new Job(
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

  public static Job create(String name, String simulationCommand, String versionCommand) {
    return null;
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
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
}
