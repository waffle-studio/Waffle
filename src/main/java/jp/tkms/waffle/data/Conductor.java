package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.List;
import java.util.UUID;

public class Conductor extends ProjectData implements DataDirectory {
  protected static final String TABLE_NAME = "conductor";
  protected static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_CONDUCTOR_TYPE = "conductor_type";
  private static final String KEY_SCRIPT = "script_file";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_EXT_JSON = ".json";
  private static final String KEY_EXT_RUBY = ".rb";

  private String conductorType = null;
  private String scriptFileName = null;
  private String defaultVariables = null;

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

  @Override
  protected Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_CONDUCTOR + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_CONDUCTOR);
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

    if (conductor[0] == null && Files.exists(project.getConductorDirectoryPath().resolve(name))) {
      conductor[0] = create(project, name);
    }

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

    try {
      Files.list(project.getConductorDirectoryPath()).forEach(path -> {
        if (Files.isDirectory(path)) {
          simulatorList.add(getInstanceByName(project, path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*
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
     */

    return simulatorList;
  }

  public static Conductor create(Project project, String name) {
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
          statement.setString(3, RubyConductor.class.getCanonicalName());
          statement.setString(4, "");
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(conductor.getDirectoryPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (! Files.exists(conductor.getScriptPath())) {
      new RubyConductor().prepareConductor(conductor);
    }
    //abstractConductor.prepareConductor(conductor);

    if (! Files.exists(conductor.getDirectoryPath().resolve(KEY_LISTENER))) {
      try {
        Files.createDirectories(conductor.getDirectoryPath().resolve(KEY_LISTENER));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      conductor.getArrayFromProperty(KEY_LISTENER);
    } catch (Exception e) {
      conductor.putNewArrayToProperty(KEY_LISTENER);
    }

    return conductor;
  }

  @Override
  public Path getDirectoryPath() {
    return getProject().getConductorDirectoryPath().resolve(name);
  }

  public String getScriptFileName() {
    return "main.rb";
  }

  public Path getScriptPath() {
    return getDirectoryPath().resolve(getScriptFileName());
  }

  public String getListenerScriptFileName(String name) {
    return name + KEY_EXT_RUBY;
  }

  public Path getListenerScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_LISTENER).resolve(getListenerScriptFileName(name));
  }

  public String getListenerScript(String name) {
    return getFileContents(KEY_LISTENER + File.separator + getListenerScriptFileName(name));
  }

  public List<String> getListenerNameList() {
    List<String> list = null;
    try {
      JSONArray array = getArrayFromProperty(KEY_LISTENER);
      list = Arrays.asList(array.toList().toArray(new String[array.toList().size()]));
      for (String name : list) {
        if (! Files.exists(getListenerScriptPath(name))) {
          removeFromArrayOfProperty(KEY_LISTENER, name);
        }
      }
    } catch (JSONException e) {
    }
    return list;
  }

  public String getConductorType() {
    if (conductorType == null) {
      conductorType = getStringFromDB(KEY_CONDUCTOR_TYPE);
    }
    return conductorType;
  }

  public JSONObject getDefaultVariables() {
    if (defaultVariables == null) {
      defaultVariables = getFileContents(KEY_DEFAULT_VARIABLES + KEY_EXT_JSON);
      if (defaultVariables.equals("")) {
        defaultVariables = "{}";
        try {
          FileWriter filewriter = new FileWriter(getDirectoryPath().resolve(KEY_DEFAULT_VARIABLES + KEY_EXT_JSON).toFile());
          filewriter.write(defaultVariables);
          filewriter.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return new JSONObject(defaultVariables);
  }

  public void setDefaultVariables(String json) {
    try {
      JSONObject object = new JSONObject(json);
      updateFileContents(KEY_DEFAULT_VARIABLES + KEY_EXT_JSON, object.toString(2));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getMainScript() {
    String mainScript = "";
    try {
      mainScript = new String(Files.readAllBytes(getScriptPath()));
    } catch (IOException e) {
    }
    return mainScript;
  }

  public void createNewListener(String name) {
    Path path = getListenerScriptPath(name);
    if (! Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(ResourceFile.getContents("/ruby_listener_template.rb"));
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    putToArrayOfProperty(KEY_LISTENER, name);
  }

  public void updateListenerScript(String name, String script) {
    Path path = getListenerScriptPath(name);
    if (Files.exists(path)) {
      try {
        FileWriter filewriter = new FileWriter(path.toFile());
        filewriter.write(script);
        filewriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void updateMainScript(String contents) {
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

  public boolean checkSyntax() {
    String mainScriptSyntaxError = RubyConductor.checkSyntax(getScriptPath());
    if (! "".equals(mainScriptSyntaxError)) {
      return false;
    }

    for (File child : getDirectoryPath().toFile().listFiles()) {
      String fileName = child.getName();
      if (child.isFile() && fileName.startsWith(KEY_LISTENER + "-") && fileName.endsWith(KEY_EXT_RUBY)) {
        String scriptSyntaxError = RubyConductor.checkSyntax(child.toPath());
        if (! "".equals(scriptSyntaxError)) {
          return false;
        }
      }
    }

    return true;
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
                "id,name," + KEY_SCRIPT + "," + KEY_CONDUCTOR_TYPE + "," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
