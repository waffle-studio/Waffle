package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ActorGroup extends ProjectData implements DataDirectory {
  protected static final String TABLE_NAME = "conductor";
  protected static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_CONDUCTOR_TYPE = "conductor_type";
  private static final String KEY_SCRIPT = "script_file";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_ACTOR = "actor";
  public static final String KEY_REPRESENTATIVE_ACTOR = "representative_actor";
  private static final String RUBY_ACTOR_TEMPLATE_RB = "/ruby_actor_template.rb";
  public static final String KEY_REPRESENTATIVE_ACTOR_NAME = "#";

  private String conductorType = null;
  private String defaultVariables = null;

  public ActorGroup(Project project) {
    super(project);
  }

  public static ArrayList<String> getConductorNameList() {
    return new ArrayList<>(Arrays.asList(
      RubyConductor.class.getCanonicalName()
    ));
  }

  public ActorGroup(Project project, UUID id, String name) {
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

  public static ActorGroup getInstance(Project project, String id) {
    final ActorGroup[] conductor = {null};

    handleDatabase(new ActorGroup(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ActorGroup(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ActorGroup getInstanceByName(Project project, String name) {
    final ActorGroup[] conductor = {null};

    handleDatabase(new ActorGroup(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ActorGroup(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    if (conductor[0] == null && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      conductor[0] = create(project, name);
    }

    return conductor[0];
  }

  public static ActorGroup find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  public static ArrayList<ActorGroup> getList(Project project) {
    ArrayList<ActorGroup> simulatorList = new ArrayList<>();

    try {
      Files.list(getBaseDirectoryPath(project)).forEach(path -> {
        if (Files.isDirectory(path)) {
          simulatorList.add(getInstanceByName(project, path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
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

  public static ActorGroup create(Project project, String name) {
    ActorGroup conductor = new ActorGroup(project, UUID.randomUUID(), name);

    if (
      handleDatabase(new ActorGroup(project), new Handler() {
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
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(conductor.getRepresentativeActorScriptPath())) {
      conductor.updateRepresentativeActorScript(null);
    }
    //abstractConductor.prepareConductor(conductor);

    if (! Files.exists(conductor.getDirectoryPath().resolve(KEY_ACTOR))) {
      try {
        Files.createDirectories(conductor.getDirectoryPath().resolve(KEY_ACTOR));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    try {
      conductor.getArrayFromProperty(KEY_ACTOR);
    } catch (Exception e) {
      conductor.putNewArrayToProperty(KEY_ACTOR);
    }

    return conductor;
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
  }

  public Path getRepresentativeActorScriptPath() {
    return getDirectoryPath().resolve(KEY_REPRESENTATIVE_ACTOR + Constants.EXT_RUBY);
  }

  public String getRepresentativeActorScript() {
    return getFileContents(getRepresentativeActorScriptPath());
  }

  public Path getActorScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_ACTOR).resolve(name + Constants.EXT_RUBY);
  }

  public String getActorScript(String name) {
    return getFileContents(getActorScriptPath(name));
  }

  public List<String> getActor1NameList() {
    List<String> list = null;
    try {
      JSONArray array = getArrayFromProperty(KEY_ACTOR);
      list = Arrays.asList(array.toList().toArray(new String[array.toList().size()]));
      for (String name : list) {
        if (! Files.exists(getActorScriptPath(name))) {
          removeFromArrayOfProperty(KEY_ACTOR, name);
        }
      }
    } catch (JSONException e) {
    }
    return list;
  }

  public JSONObject getDefaultVariables() {
    final String fileName = KEY_DEFAULT_VARIABLES + Constants.EXT_JSON;
    if (defaultVariables == null) {
      defaultVariables = getFileContents(fileName);
      if (defaultVariables.equals("")) {
        defaultVariables = "{}";
        createNewFile(fileName);
        updateFileContents(fileName, defaultVariables);
      }
    }
    return new JSONObject(defaultVariables);
  }

  public void setDefaultVariables(String json) {
    try {
      JSONObject object = new JSONObject(json);
      updateFileContents(KEY_DEFAULT_VARIABLES + Constants.EXT_JSON, object.toString(2));
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }

  public void createNewActor(String name) {
    Path path = getActorScriptPath(name);
    createNewFile(path);
    updateFileContents(path, ResourceFile.getContents(RUBY_ACTOR_TEMPLATE_RB));
    putToArrayOfProperty(KEY_ACTOR, name);
  }

  public void updateActorScript(String name, String script) {
    updateFileContents(getActorScriptPath(name), script);
  }

  public void updateRepresentativeActorScript(String contents) {
    Path path = getRepresentativeActorScriptPath();
    if (Files.exists(path)) {
      updateFileContents(path, contents);
    } else {
      createNewFile(path);
      updateFileContents(path, ResourceFile.getContents(RUBY_ACTOR_TEMPLATE_RB));
    }
  }

  public boolean checkSyntax() {
    /*
    String mainScriptSyntaxError = RubyConductor.checkSyntax(getScriptPath());
    if (! "".equals(mainScriptSyntaxError)) {
      return false;
    }

    for (File child : getDirectoryPath().toFile().listFiles()) {
      String fileName = child.getName();
      if (child.isFile() && fileName.startsWith(KEY_ACTOR + "-") && fileName.endsWith(KEY_EXT_RUBY)) {
        String scriptSyntaxError = RubyConductor.checkSyntax(child.toPath());
        if (! "".equals(scriptSyntaxError)) {
          return false;
        }
      }
    }

     */

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
