package jp.tkms.waffle.data;

import java.io.File;
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

public class Trials extends ProjectData {
  protected static final String TABLE_NAME = "trials";
  public static final String ROOT_NAME = "ROOT";
  private static final String KEY_PARENT = "parent";

  private UUID id = null;
  private String shortId;
  private String name;

  private Project project;

  public Trials(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Trials getInstance(Project project, String id) {
    Trials trials = null;
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        trials = new Trials(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return trials;
  }

  public static Trials getRootInstance(Project project) {
    Trials trials = null;
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      ResultSet resultSet
        = db.executeQuery("select id,name from " + TABLE_NAME + " where name='" + ROOT_NAME + "';");
      while (resultSet.next()) {
        trials = new Trials(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return trials;
  }

  public static ArrayList<Trials> getList(Project project, Trials parent) {
    ArrayList<Trials> list = new ArrayList<>();
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_PARENT + "=?;");
      statement.setString(1, parent.getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        list.add(new Trials(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name"))
        );
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static Trials create(Project project, Trials parent, String name) {
    Trials trials = new Trials(project, UUID.randomUUID(), name);
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,name," + KEY_PARENT + ") values(?,?.?);");
      statement.setString(1, trials.getId());
      statement.setString(2, trials.getName());
      statement.setString(2, parent.getId());
      statement.execute();
      db.commit();
      db.close();

      //Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return trials;
  }

  public Path getLocation() {
    Path path = Paths.get(project.getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return null;
  }

  @Override
  protected DatabaseUpdater getWorkDatabaseUpdater() {
    return workDatabaseUpdater;
  }

  private static DatabaseUpdater workDatabaseUpdater = new DatabaseUpdater() {
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
            db.execute("create table " + TABLE_NAME + "(" +
              "id,name," + KEY_PARENT + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        },
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            String scriptName = "test";
            PreparedStatement statement = db.preparedStatement("insert into " + TABLE_NAME + "(" +
              "id,name," +
              KEY_PARENT +
              ") values(?,?,?);");
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, ROOT_NAME);
            statement.setString(3, "");
            statement.execute();
          }
        }
      ));
    }
  };
}
