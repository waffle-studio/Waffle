package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.TestConductor;

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

public class Conductor extends ProjectData {
  protected static final String TABLE_NAME = "conductor";
  private static final String KEY_CONDUCTOR_TYPE = "conductor_type";
  private static final String KEY_SCRIPT = "script_file";

  enum ConductorType {Test}

  private String scriptFileName = null;

  public Conductor(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Conductor getInstance(Project project, String id) {
    Conductor conductor = null;
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        conductor = new Conductor(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return conductor;
  }

  public static ArrayList<Conductor> getList(Project project) {
    ArrayList<Conductor> simulatorList = new ArrayList<>();
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        simulatorList.add(new Conductor(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name"))
        );
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return simulatorList;
  }

  public static Conductor create(Project project, String name, String scriptFileName) {
    Conductor simulator = new Conductor(project, UUID.randomUUID(), name);
    System.out.println(simulator.getName());
    System.out.println(simulator.name);
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,"
        + KEY_SCRIPT
        + ") values(?,?,?);");
      statement.setString(1, simulator.getId());
      statement.setString(2, simulator.getName());
      statement.setString(3, scriptFileName);
      statement.execute();
      db.commit();
      db.close();

      Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return simulator;
  }

  public Path getLocation() {
    Path path = Paths.get(project.getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  public String getScriptFileName() {
    if (scriptFileName == null) {
      scriptFileName = getFromDB(KEY_SCRIPT);
    }
    return scriptFileName;
  }

  public void start() {
    AbstractConductor abstractConductor = null;
    String type = getFromDB(KEY_CONDUCTOR_TYPE);
    if (type.equals(ConductorType.Test.name())) {
      abstractConductor = new TestConductor();
    }

    if (abstractConductor != null) {
      abstractConductor.run(this);
    }
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
              "id,name," + KEY_SCRIPT + "," + KEY_CONDUCTOR_TYPE + "," +
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
              KEY_CONDUCTOR_TYPE + "," +
              KEY_SCRIPT +
              ") values(?,?,?,?);");
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, "Trial Submitter");
            statement.setString(3, ConductorType.Test.name());
            statement.setString(4, scriptName);
            statement.execute();
          }
        }
      ));
    }
  };
}
