package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.RubyConductor;
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

  private String conductorType = null;
  private String scriptFileName = null;

  public static ArrayList<String> getConductorNameList() {
    return new ArrayList<>(Arrays.asList(
      RubyConductor.class.getCanonicalName(),
      TestConductor.class.getCanonicalName()
    ));
  }

  public Conductor(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Conductor getInstance(Project project, String id) {
    final Conductor[] conductor = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new Conductor(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ArrayList<Conductor> getList(Project project) {
    ArrayList<Conductor> simulatorList = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new Conductor(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return simulatorList;
  }

  public static Conductor create(Project project, String name, Class<AbstractConductor> conductorClass, String scriptFileName) {
    Conductor simulator = new Conductor(project, UUID.randomUUID(), name);

    if (
      handleWorkDB(project, workUpdater, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name," +
            KEY_CONDUCTOR_TYPE + ","
            + KEY_SCRIPT + ") values(?,?,?,?);");
          statement.setString(1, simulator.getId());
          statement.setString(2, simulator.getName());
          statement.setString(3, conductorClass.getCanonicalName());
          statement.setString(4, scriptFileName);
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
    Path path = Paths.get(project.getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + getUnifiedName()
    );
    return path;
  }

  public String getScriptFileName() {
    if (scriptFileName == null) {
      scriptFileName = getFromDB(KEY_SCRIPT);
    }
    return scriptFileName;
  }

  public String getConductorType() {
    if (conductorType == null) {
      conductorType = getFromDB(KEY_CONDUCTOR_TYPE);
    }
    return conductorType;
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
        }
      ));
    }
  };
}
