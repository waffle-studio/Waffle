package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
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

public class Conductor extends ProjectData {
  protected static final String TABLE_NAME = "conductor";
  private static final String KEY_CONDUCTOR_TYPE = "conductor_type";
  private static final String KEY_SCRIPT = "script_file";
  private static final String KEY_ARGUMENTS = "arguments";

  private String conductorType = null;
  private String scriptFileName = null;
  private String arguments = null;

  public Conductor(Project project) {
    super(project);
  }

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

    handleDatabase(new Conductor(project), new Handler() {
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

  public static Conductor getInstanceByName(Project project, String name) {
    final Conductor[] conductor = {null};

    handleDatabase(new Conductor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
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

  public static Conductor find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  public static ArrayList<Conductor> getList(Project project) {
    ArrayList<Conductor> simulatorList = new ArrayList<>();

    handleDatabase(new Conductor(project), new Handler() {
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

  public static Conductor create(Project project, String name, AbstractConductor abstractConductor, String scriptFileName) {
    Conductor conductor = new Conductor(project, UUID.randomUUID(), name);

    if (
      handleDatabase(new Conductor(project), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name," +
            KEY_CONDUCTOR_TYPE + ","
            + KEY_SCRIPT + ") values(?,?,?,?);");
          statement.setString(1, conductor.getId());
          statement.setString(2, conductor.getName());
          statement.setString(3, abstractConductor.getClass().getCanonicalName());
          statement.setString(4, scriptFileName);
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(conductor.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    abstractConductor.prepareConductor(conductor);

    return conductor;
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
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

  public Path getScriptPath() {
    return getLocation().resolve(getScriptFileName());
  }

  public String getConductorType() {
    if (conductorType == null) {
      conductorType = getFromDB(KEY_CONDUCTOR_TYPE);
    }
    return conductorType;
  }

  public JSONObject getArguments() {
    if (arguments == null) {
      arguments = getFromDB(KEY_ARGUMENTS);
    }
    return new JSONObject(arguments);
  }

  public void setArguments(String json) {
    JSONObject args = new JSONObject(json);
    if (args != null) {
      arguments = args.toString();

      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_ARGUMENTS + "=? where " + KEY_ID + "=?;");
          statement.setString(1, arguments);
          statement.setString(2, getId());
          statement.execute();
        }
      });
    }
  }

  public String getMainScriptContents() {
    String mainScript = "";
    try {
      mainScript = new String(Files.readAllBytes(getScriptPath()));
    } catch (IOException e) {
    }
    return mainScript;
  }

  public void updateMainScriptContents(String contents) {
    if (Files.exists(getScriptPath())) {
      try {
        FileWriter filewriter = new FileWriter(getScriptPath().toFile());
        filewriter.write(contents);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
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
      ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("create table " + TABLE_NAME + "(" +
                "id,name," + KEY_SCRIPT + "," + KEY_CONDUCTOR_TYPE + "," + KEY_ARGUMENTS + " default '{}'," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
