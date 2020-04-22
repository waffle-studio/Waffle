package jp.tkms.waffle.data;

import jp.tkms.waffle.collector.AbstractResultCollector;
import jp.tkms.waffle.collector.JsonResultCollector;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.Sql;

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
  public static final String BIN_DIR = "bin";

  protected static final String TABLE_NAME = "simulator";
  private static final String KEY_SIMULATION_COMMAND = "simulation_command";

  private String simulationCommand = null;
  private String versionCommand = null;

  public Simulator(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public Simulator(Project project) {
    super(project);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Simulator getInstance(Project project, String id) {
    final Simulator[] simulator = {null};

    handleDatabase(new Simulator(project), new Handler() {
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

    handleDatabase(new Simulator(project), new Handler() {
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

  public static Simulator find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  public static ArrayList<Simulator> getList(Project project) {
    ArrayList<Simulator> simulatorList = new ArrayList<>();

    handleDatabase(new Simulator(project), new Handler() {
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

  public static Simulator create(Project project, String name, String simulationCommand) {
    Simulator simulator = new Simulator(project, UUID.randomUUID(), name);

    if (
      handleDatabase(new Simulator(project), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,"
            + KEY_SIMULATION_COMMAND
            + ") values(?,?,?);");
          statement.setString(1, simulator.getId());
          statement.setString(2, simulator.getName());
          statement.setString(3, simulationCommand);
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(simulator.getLocation());
        Files.createDirectories(simulator.getBinDirectoryLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }

      ParameterExtractor.create(simulator, "command arguments", ResourceFile.getContents("/command_arguments.rb"));
      ResultCollector.create(simulator, "_output.json", AbstractResultCollector.getInstance(JsonResultCollector.class.getCanonicalName()));
    }

    return simulator;
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  public Path getBinDirectoryLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId + File.separator + BIN_DIR
    );
    return path;
  }

  public String getSimulationCommand() {
    if (simulationCommand == null) {
      simulationCommand = getFromDB(KEY_SIMULATION_COMMAND);
    }
    return simulationCommand;
  }

  public void setSimulatorCommand(String command) {
    if (handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, getTableName(),
          Sql.Value.equal(KEY_SIMULATION_COMMAND, command)).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    })) {
      simulationCommand = command;
    }
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
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
                "id,name," + KEY_SIMULATION_COMMAND + "," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
