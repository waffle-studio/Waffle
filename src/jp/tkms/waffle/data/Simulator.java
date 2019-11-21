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

public class Simulator extends ProjectData {
  protected static final String TABLE_NAME = "simulator";
  private static final String KEY_SIMULATION_COMMAND = "simulation_command";
  private static final String KEY_VERSION_COMMAND = "version_command";

  private String simulationCommand = null;
  private String versionCommand = null;

  public Simulator(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Simulator getInstance(Project project, String id) {
    final Simulator[] simulator = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          simulator[0] = new Simulator(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return simulator[0];
  }

  public static Simulator getInstanceByName(Project project, String name) {
    final Simulator[] simulator = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where name=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          simulator[0] = new Simulator(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return simulator[0];
  }

  public static ArrayList<Simulator> getList(Project project) {
    ArrayList<Simulator> simulatorList = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new Simulator(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return simulatorList;
  }

  public static Simulator create(Project project, String name, String simulationCommand, String versionCommand) {
    Simulator simulator = new Simulator(project, UUID.randomUUID(), name);

    if (
      handleWorkDB(project, workUpdater, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,"
            + KEY_SIMULATION_COMMAND + "," + KEY_VERSION_COMMAND
            + ") values(?,?,?,?);");
          statement.setString(1, simulator.getId());
          statement.setString(2, simulator.getName());
          statement.setString(3, simulationCommand);
          statement.setString(4, versionCommand);
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(simulator.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return simulator;
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  public String getSimulationCommand() {
    if (simulationCommand == null) {
      simulationCommand = getFromDB(KEY_SIMULATION_COMMAND);
    }
    return simulationCommand;
  }

  public String getVersionCommand() {
    if (versionCommand == null) {
      versionCommand = getFromDB(KEY_VERSION_COMMAND);
    }
    return versionCommand;
  }

  @Override
  protected Updater getMainUpdater() {
    return null;
  }

  @Override
  protected Updater getWorkUpdater() {
    return workUpdater;
  }

  private static Updater workUpdater = new Updater() {
    @Override
    String tableName() {
      return TABLE_NAME;
    }

    @Override
    ArrayList<Updater.UpdateTask> updateTasks() {
      return new ArrayList<Updater.UpdateTask>(Arrays.asList(
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            db.execute("create table " + TABLE_NAME + "(" +
              "id,name," + KEY_SIMULATION_COMMAND + "," + KEY_VERSION_COMMAND + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
