package jp.tkms.waffle.data.project.conductor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.HasName;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.ProjectData;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.data.util.WrappedJsonArray;
import jp.tkms.waffle.exception.ChildProcedureNotFoundException;
import jp.tkms.waffle.exception.InvalidInputException;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.script.ruby.RubyScriptProcessor;

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
  public static final String MAIN_PROCEDURE_SHORT_ALIAS = "#";
  public static final String MAIN_PROCEDURE_ALIAS = "MAIN_PROCEDURE";

  private static final Map<String, Conductor> instanceMap = new WeakHashMap<>();

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
    return getPath().resolve(CONDUCTOR + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getPath().resolve(CONDUCTOR);
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

  public static Conductor create(Project project, String name) throws InvalidInputException {
    name = FileName.removeRestrictedCharacters(name);
    if (name.length() <= 0) {
      throw new InvalidInputException(name);
    }

    Conductor conductor = getInstance(project, name);
    if (conductor == null) {
      conductor = new Conductor(project, name);
    }

    return conductor;
  }

  protected void initialise() {
    try {
      Files.createDirectories(getPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    if (! Files.exists(getMainProcedureScriptPath())) {
      updateRepresentativeActorScript(null);
    }

    if (! Files.exists(getPath().resolve(KEY_CHILD))) {
      try {
        Files.createDirectories(getPath().resolve(KEY_CHILD));
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
  public Path getPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
  }

  public Path getMainProcedureScriptPath() {
    return getPath().resolve(getStringFromProperty(KEY_MAIN_PROCEDURE_FILENAME, MAIN_PROCEDURE_FILENAME));
  }

  public String getMainProcedureScript() {
    return getFileContents(getMainProcedureScriptPath());
  }

  public Path getChildProcedureScriptPath(String name) {
    return getPath().resolve(KEY_CHILD).resolve(name);
  }

  public String getChildProcedureScript(String name) throws ChildProcedureNotFoundException {
    Path path = getChildProcedureScriptPath(name);
    if (!Files.exists(path)) {
      removeChildProcedure(name);
      throw new ChildProcedureNotFoundException(path);
    }
    return getFileContents(getChildProcedureScriptPath(name));
  }

  public List<String> getChildProcedureNameList() {
    WrappedJsonArray array = getArrayFromProperty(KEY_CHILD);
    if (array == null) {
      putNewArrayToProperty(KEY_CHILD);
      array = new WrappedJsonArray();
    }

    List<String> list = new ArrayList<>();
    for (Object o : array) {
      String name = o.toString();
      list.add(name);
      if (! Files.exists(getChildProcedureScriptPath(name))) {
        removeFromArrayOfProperty(KEY_CHILD, name);
      }
    }
    return list;
  }

  public WrappedJson getDefaultVariables() {
    final String fileName = KEY_DEFAULT_VARIABLES + Constants.EXT_JSON;
    if (defaultVariables == null) {
      defaultVariables = getFileContents(fileName);
      if (defaultVariables.equals("")) {
        defaultVariables = "{}";
        createNewFile(fileName);
        updateFileContents(fileName, defaultVariables);
      }
    }
    return new WrappedJson(defaultVariables);
  }

  public void setDefaultVariables(String json) {
    try {
      JsonObject object = Json.parse(json).asObject();
      updateFileContents(KEY_DEFAULT_VARIABLES + Constants.EXT_JSON, object.toString(WriterConfig.PRETTY_PRINT));
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

  public void removeChildProcedure(String name) {
    removeFromArrayOfProperty(KEY_CHILD, name);
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

  WrappedJson propertyStoreCache = null;
  @Override
  public WrappedJson getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(WrappedJson cache) {
    propertyStoreCache = cache;
  }
}
