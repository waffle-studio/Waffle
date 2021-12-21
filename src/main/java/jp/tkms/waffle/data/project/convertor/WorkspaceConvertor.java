package jp.tkms.waffle.data.project.convertor;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.HasName;
import jp.tkms.waffle.data.HasNote;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.ProjectData;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.exception.InvalidInputException;
import jp.tkms.waffle.script.ScriptProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public class WorkspaceConvertor extends ProjectData implements DataDirectory, PropertyFile, HasName, HasNote {
  public static final String WORKSPACE_CONVERTOR = "WORKSPACE_CONVERTOR";
  public static final String KEY_FILENAME = "FILENAME";
  public static final String DEFAULT_FILENAME = WORKSPACE_CONVERTOR + ".rb";

  private static final Map<String, WorkspaceConvertor> instanceMap = new WeakHashMap<>();

  private String name = null;

  public WorkspaceConvertor(Project project, String name) {
    super(project);
    this.name = name;

    if (this.getClass().getConstructors()[0].getDeclaringClass().equals(WorkspaceConvertor.class)) {
      initialise();
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Path getPropertyStorePath() {
    return getPath().resolve(WORKSPACE_CONVERTOR + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getPath().resolve(WORKSPACE_CONVERTOR);
  }

  public static WorkspaceConvertor getInstance(Project project, String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      WorkspaceConvertor convertor = instanceMap.get(name);
      if (convertor == null) {
        convertor = new WorkspaceConvertor(project, name);
      }
      return convertor;
    }
    return null;
  }

  public static WorkspaceConvertor find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<WorkspaceConvertor> getList(Project project) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(project), name -> {
      return getInstance(project, name.toString());
    });
  }

  public static WorkspaceConvertor create(Project project, String name) throws InvalidInputException {
    name = FileName.removeRestrictedCharacters(name);
    if (name.length() <= 0) {
      throw new InvalidInputException(name);
    }

    WorkspaceConvertor convertor = getInstance(project, name);
    if (convertor == null) {
      convertor = new WorkspaceConvertor(project, name);
    }

    return convertor;
  }

  protected void initialise() {
    try {
      Files.createDirectories(getPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    if (! Files.exists(getScriptPath())) {
      updateScript(null);
    }
  }

  @Override
  public Path getPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
  }

  public Path getScriptPath() {
    return getPath().resolve(getStringFromProperty(KEY_FILENAME, DEFAULT_FILENAME));
  }

  public String getScript() {
    return getFileContents(getScriptPath());
  }

  public void updateScript(String contents) {
    Path path = getScriptPath();
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
