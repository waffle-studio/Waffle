package jp.tkms.waffle.data.project.conductor;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.HasName;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.ProjectData;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.script.ruby.RubyScriptProcessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Conductor extends ProjectData implements DataDirectory, PropertyFile, HasName {
  public static final String CONDUCTOR = "CONDUCTOR";
  private static final String KEY_DEFAULT_VARIABLES = "DEFAULT_VARIABLES";
  private static final String KEY_CHILD = "CHILD";
  public static final String MAIN_PROCEDURE_FILENAME = "MAIN_PROCEDURE.rb";
  public static final String KEY_MAIN_PROCEDURE_FILENAME = "MAIN_PROCEDURE";
  public static final String MAIN_PROCEDURE_ALIAS = "#";

  private static final HashMap<String, Conductor> instanceMap = new HashMap<>();

  private String name = null;
  private String defaultVariables = null;

  public Conductor(Project project, String name) {
    super(project);
    this.name = name;

    if (this.getClass().getConstructors()[0].getDeclaringClass().equals(Conductor.class)) {
      initialise();
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(CONDUCTOR + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(CONDUCTOR);
  }

  public static Conductor getInstance(Project project, String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      Conductor conductor = instanceMap.get(name);
      if (conductor == null) {
        conductor = new Conductor(project, name);
      }
      return conductor;
    }
    return null;
  }

  public static Conductor find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<Conductor> getList(Project project) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(project), name -> {
      return getInstance(project, name.toString());
    });
  }

  public static Conductor create(Project project, String name) {
    name = FileName.removeRestrictedCharacters(name);

    Conductor conductor = getInstance(project, name);
    if (conductor == null) {
      conductor = new Conductor(project, name);
    }

    return conductor;
  }

  protected void initialise() {
    try {
      Files.createDirectories(getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    if (! Files.exists(getMainProcedureScriptPath())) {
      updateRepresentativeActorScript(null);
    }

    if (! Files.exists(getDirectoryPath().resolve(KEY_CHILD))) {
      try {
        Files.createDirectories(getDirectoryPath().resolve(KEY_CHILD));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    try {
      getArrayFromProperty(KEY_CHILD);
    } catch (Exception e) {
      putNewArrayToProperty(KEY_CHILD);
    }
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
  }

  public Path getMainProcedureScriptPath() {
    return getDirectoryPath().resolve(getStringFromProperty(KEY_MAIN_PROCEDURE_FILENAME, MAIN_PROCEDURE_FILENAME));
  }

  public String getMainProcedureScript() {
    return getFileContents(getMainProcedureScriptPath());
  }

  public Path getChildProcedureScriptPath(String name) {
    return getDirectoryPath().resolve(KEY_CHILD).resolve(name);
  }

  public String getChildProcedureScript(String name) {
    return getFileContents(getChildProcedureScriptPath(name));
  }

  public List<String> getChildProcedureNameList() {
    List<String> list = null;
    try {
      JSONArray array = getArrayFromProperty(KEY_CHILD);
      if (array == null) {
        putNewArrayToProperty(KEY_CHILD);
        array = new JSONArray();
      }
      list = Arrays.asList(array.toList().toArray(new String[array.toList().size()]));
      for (String name : list) {
        if (! Files.exists(getChildProcedureScriptPath(name))) {
          removeFromArrayOfProperty(KEY_CHILD, name);
        }
      }
    } catch (JSONException e) {
    }

    if (list == null) {
      return new ArrayList<>();
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
      defaultVariables = object.toString();
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }

  public String createNewChildProcedure(String name) {
    if (!ScriptProcessor.CLASS_NAME_MAP.containsKey(name.replaceFirst("^.*\\.", "."))) {
      name = name + RubyScriptProcessor.EXTENSION;
    }

    Path path = getChildProcedureScriptPath(name);
    createNewFile(path);
    updateFileContents(path, ScriptProcessor.getProcessor(path).procedureTemplate());
    putToArrayOfProperty(KEY_CHILD, name);

    return name;
  }

  public void updateActorScript(String name, String script) {
    updateFileContents(getChildProcedureScriptPath(name), script);
  }

  public void updateRepresentativeActorScript(String contents) {
    Path path = getMainProcedureScriptPath();
    if (Files.exists(path)) {
      updateFileContents(path, contents);
    } else {
      createNewFile(path);
      updateFileContents(path, ScriptProcessor.getProcessor(path).procedureTemplate());
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
