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
import java.util.UUID;

public class Simulator {
  private static final String KEY_SIMULATION_COMMAND = "simulation_command";
  private static final String KEY_VERSION_COMMAND = "version_command";

  private UUID id = null;
  private String shortId;
  private String name;

  private String simulationCommand = null;
  private String versionCommand = null;

  private Project project;

  public Simulator(Project project, UUID id, String name) {
    this.project = project;
    this.id = id;
    this.shortId = id.toString().replaceFirst("-.*$", "");
    this.name = name;
  }

  public Simulator(Project project, String id) {
    this.project = project;
    try {
      Database db = getWorkDB(project);
      PreparedStatement statement = db.preparedStatement("select id,name from simulator where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        this.id = UUID.fromString(resultSet.getString("id"));
        this.shortId = this.id.toString().replaceFirst("-.*$", "");
        this.name = resultSet.getString("name");
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<Simulator> getSimulatorList(Project project) {
    ArrayList<Simulator> simulatorList = new ArrayList<>();
    try {
      Database db = getWorkDB(project);
      ResultSet resultSet = db.executeQuery("select id,name from simulator;");
      while (resultSet.next()) {
        simulatorList.add(new Simulator(
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

  public static Simulator create(Project project, String name, String simulationCommand, String versionCommand) {
    Simulator simulator = new Simulator(project, UUID.randomUUID(), name);
    System.out.println(simulator.getName());
    System.out.println(simulator.name);
    try {
      Database db = getWorkDB(project);
      PreparedStatement statement
        = db.preparedStatement("insert into simulator(id,name,"
        + KEY_SIMULATION_COMMAND + "," + KEY_VERSION_COMMAND
        + ") values(?,?,?,?);");
      statement.setString(1, simulator.getId());
      statement.setString(2, simulator.getName());
      statement.setString(3, simulationCommand);
      statement.setString(4, versionCommand);
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

  private static Database getWorkDB(Project project) {
    Database db = Database.getWorkDB(project);
    updateWorkDB(db);
    return db;
  }

  private static void updateWorkDB(Database db) {
    try {
      int currentVersion = db.getVersion("simulator");
      int version = 0;

      if (currentVersion <= version++) {
        db.execute("create table simulator(" +
          "id,name," + KEY_SIMULATION_COMMAND + "," + KEY_VERSION_COMMAND + "," +
          "timestamp_create timestamp default (DATETIME('now','localtime'))" +
          ");");
      }

      db.setVersion("simulator", version);
      db.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private String getFromDB(String key) {
    String result = null;
    try {
      Database db = getWorkDB(project);
      PreparedStatement statement = db.preparedStatement("select " + key + " from simulator where id=?;");
      statement.setString(1, getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        result = resultSet.getString(key);
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  public boolean isValid() {
    return id != null;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id.toString();
  }

  public String getShortId() {
    return shortId;
  }

  public Project getProject() {
    return project;
  }

  public Path getLocation() {
    Path path = Paths.get(project.getLocation().toAbsolutePath() + File.separator +
      "simulator" + File.separator + name + '_' + shortId
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
}
