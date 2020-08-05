package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.ResourceFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ActorGroup extends ProjectData implements DataDirectory, PropertyFile {
  protected static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_ACTOR = "actor";
  public static final String KEY_REPRESENTATIVE_ACTOR = "representative_actor";
  private static final String RUBY_ACTOR_TEMPLATE_RB = "/ruby_actor_template.rb";
  public static final String KEY_REPRESENTATIVE_ACTOR_NAME = "#";

  private static final HashMap<String, ActorGroup> instanceMap = new HashMap<>();

  private String name = null;
  private String defaultVariables = null;

  public ActorGroup(Project project, String name) {
    super(project);
    this.name = name;
    instanceMap.put(name, this);
    initialise();
  }

  public String getName() {
    return name;
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_CONDUCTOR + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(KEY_CONDUCTOR);
  }

  public static ActorGroup getInstance(Project project, String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      ActorGroup actorGroup = instanceMap.get(name);
      if (actorGroup == null) {
        actorGroup = new ActorGroup(project, name);
      }
      return actorGroup;
    }
    return null;
  }

  public static ActorGroup find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<ActorGroup> getList(Project project) {
    ArrayList<ActorGroup> simulatorList = new ArrayList<>();

    for (File file : getBaseDirectoryPath(project).toFile().listFiles()) {
      if (file.isDirectory()) {
        simulatorList.add(getInstance(project, file.getName()));
      }
    }

    return simulatorList;
  }

  public static ActorGroup create(Project project, String name) {
    name = FileName.removeRestrictedCharacters(name);

    ActorGroup actorGroup = getInstance(project, name);
    if (actorGroup == null) {
      actorGroup = new ActorGroup(project, name);
    }

    return actorGroup;
  }

  private void initialise() {
    try {
      Files.createDirectories(getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    if (! Files.exists(getRepresentativeActorScriptPath())) {
      updateRepresentativeActorScript(null);
    }

    if (! Files.exists(getDirectoryPath().resolve(KEY_ACTOR))) {
      try {
        Files.createDirectories(getDirectoryPath().resolve(KEY_ACTOR));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    try {
      getArrayFromProperty(KEY_ACTOR);
    } catch (Exception e) {
      putNewArrayToProperty(KEY_ACTOR);
    }
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

  public List<String> getActorNameList() {
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
