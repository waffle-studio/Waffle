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

public class Trials extends ProjectData {
  protected static final String TABLE_NAME = "trials";

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
      Database db = getWorkDB(project, null);
      PreparedStatement statement = db.preparedStatement("select id,name from simulator where id=?;");
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

  public static ArrayList<Trials> getList(Project project) {
    ArrayList<Trials> simulatorList = new ArrayList<>();
    try {
      Database db = getWorkDB(project, null);
      ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        simulatorList.add(new Trials(
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

  public static Trials create(Project project, String name) {
    Trials simulator = new Trials(project, UUID.randomUUID(), name);
    System.out.println(simulator.getName());
    System.out.println(simulator.name);
    try {
      Database db = getWorkDB(project, null);
      PreparedStatement statement
        = db.preparedStatement("insert into simulator(id,name) values(?,?);");
      statement.setString(1, simulator.getId());
      statement.setString(2, simulator.getName());
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

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return null;
  }

  @Override
  protected DatabaseUpdater getWorkDatabaseUpdater() {
    return null;
  }
}
