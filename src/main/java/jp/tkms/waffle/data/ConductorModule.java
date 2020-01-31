package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.conductor.module.RubyCondoctorModule;
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

public class ConductorModule extends Data {
  protected static final String TABLE_NAME = "conductor_module";
  private static final String KEY_SCRIPT = "script_file";
  private static final String KEY_ARGUMENTS = "arguments";

  private String conductorType = null;
  private String scriptFileName = null;
  private String arguments = null;

  public ConductorModule() {
    super();
  }

  public ConductorModule(UUID id, String name) {
    super(id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ConductorModule getInstance(String id) {
    final ConductorModule[] conductor = {null};

    handleDatabase(new ConductorModule(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ConductorModule(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ConductorModule getInstanceByName(String name) {
    final ConductorModule[] conductor = {null};

    handleDatabase(new ConductorModule(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ConductorModule(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ConductorModule find_by_name(String name) {
    return getInstanceByName(name);
  }

  public static ArrayList<ConductorModule> getList() {
    ArrayList<ConductorModule> simulatorList = new ArrayList<>();

    handleDatabase(new ConductorModule(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          simulatorList.add(new ConductorModule(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return simulatorList;
  }

  public static ConductorModule create(String name) {
    ConductorModule module = new ConductorModule(UUID.randomUUID(), name);

    if (
      handleDatabase(new ConductorModule(), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,"
            + KEY_SCRIPT + ") values(?,?,?);");
          statement.setString(1, module.getId());
          statement.setString(2, module.getName());
          statement.setString(3, "main.rb");
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(module.getLocation());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    RubyCondoctorModule.prepareModule(module);

    return module;
  }

  public Path getLocation() {
    return Paths.get(Constants.MODULE_DIR.replaceFirst( "\\$\\{NAME\\}", getUnifiedName()));
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
                "id,name," + KEY_SCRIPT + "," + KEY_ARGUMENTS + " default '{}'," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
