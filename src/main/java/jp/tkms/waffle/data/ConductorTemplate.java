package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConductorTemplate implements DataDirectory, PropertyFile {
  protected static final String KEY_CONDUCTOR_TEMPLATE = "conductor_template";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_FUNCTIONS = "functionss";

  private String name;
  private String arguments = null;

  public ConductorTemplate(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static ConductorTemplate getInstance(String name) {
    ConductorTemplate conductorTemplate = null;

    if (Files.exists(getBaseDirectoryPath().resolve(name))) {
      conductorTemplate = new ConductorTemplate(name);

      if (! Files.exists(conductorTemplate.getMainScriptPath())) {
        conductorTemplate.createNewFile(conductorTemplate.getMainScriptPath());
        conductorTemplate.updateMainScript(ResourceFile.getContents("/ruby_conductor_template.rb"));
      }

      try {
        Files.createDirectories(conductorTemplate.getDirectoryPath().resolve(KEY_LISTENER));
      } catch (IOException e) { }

      if (conductorTemplate.getListenerNameList() == null) {
        conductorTemplate.putNewArrayToProperty(KEY_LISTENER);
      }
    }

    return conductorTemplate;
  }

  /*
  public static ConductorTemplate getInstance(String id) {
    final ConductorTemplate[] conductor = {null};

    handleDatabase(new ConductorTemplate(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ConductorTemplate(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ConductorTemplate getInstance(String name) {
    final ConductorTemplate[] conductor = {null};

    handleDatabase(new ConductorTemplate(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_NAME + "=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductor[0] = new ConductorTemplate(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductor[0];
  }

  public static ConductorTemplate find(String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(key);
    }
    return getInstance(key);
  }
  dd
   */

  public static ArrayList<ConductorTemplate> getList() {
    ArrayList<ConductorTemplate> conductorTemplates = new ArrayList<>();

    Data.initializeWorkDirectory();

    for (File file : getBaseDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        conductorTemplates.add(new ConductorTemplate(file.getName()));
      }
    }

    return conductorTemplates;
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

  public static ConductorTemplate create(String name) {
    try {
      Files.createDirectories(getBaseDirectoryPath().resolve(name));
    } catch (IOException e) { }
    return new ConductorTemplate(name);
  }

  public static Path getBaseDirectoryPath() {
    return Data.getWaffleDirectoryPath().resolve(Constants.CONDUCTOR_TEMPLATE);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_CONDUCTOR_TEMPLATE + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath().resolve(name);
  }

  public String getMainScriptFileName() {
    return "main.rb";
  }

  public Path getMainScriptPath() {
    return getDirectoryPath().resolve(getMainScriptFileName());
  }

  public String getListenerScriptFileName(String name) {
    return name + Constants.EXT_RUBY;
  }

  public Path getListenerScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_LISTENER).resolve(getListenerScriptFileName(name));
  }

  /*
  public ArrayList<String> getArguments() {
    if (arguments == null) {
      arguments = getStringFromDB(KEY_ARGUMENTS);
    }
    ArrayList<String> list = new ArrayList<>();
    for (Object o : (new JSONArray(arguments)).toList()) {
      list.add(o.toString());
    }
    return list;
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
  */


  public String getListenerScript(String name) {
    return getFileContents(getListenerScriptPath(name));
  }

  public String getMainScript() {
    return getFileContents(getMainScriptPath());
  }

  public void createNewListener(String name) {
    Path path = getListenerScriptPath(name);
    if (! Files.exists(path)) {
      createNewFile(path);
      updateListenerScript(name, ResourceFile.getContents("/ruby_actor_template.rb"));
    }
    putToArrayOfProperty(KEY_LISTENER, name);
  }

  public void updateListenerScript(String name, String script) {
    updateFileContents( getListenerScriptPath(name), script );
  }

  public void updateMainScript(String script) {
    updateFileContents(getMainScriptPath(), script);
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }
}
