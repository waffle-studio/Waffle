package jp.tkms.waffle.data.project.workspace;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.ProjectData;
import jp.tkms.waffle.data.util.FileName;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Workspace extends ProjectData implements DataDirectory, PropertyFile {
  public static final String WORKSPACE = "WORKSPACE";
  public static final String TESTRUN_WORKSPACE = ".TESTRUN_WORKSPACE";

  private String name = null;

  public Workspace(Project project, String name) {
    super(project);
    this.name = name;
    initialise();
  }

  public String getName() {
    return name;
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(WORKSPACE + Constants.EXT_JSON);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getDirectoryPath().resolve(WORKSPACE);
  }

  public static Workspace getInstance(Project project, String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      return new Workspace(project, name);
    }
    return null;
  }

  public static Workspace find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<Workspace> getList(Project project) {
    ArrayList<Workspace> workspaceList = new ArrayList<>();

    for (File file : getBaseDirectoryPath(project).toFile().listFiles()) {
      if (file.isDirectory()) {
        workspaceList.add(getInstance(project, file.getName()));
      }
    }

    return workspaceList;
  }

  public static Workspace getTestRunWorkspace(Project project) {
    Workspace workspace = getInstance(project, TESTRUN_WORKSPACE);
    if (workspace == null) {
      workspace = new Workspace(project, TESTRUN_WORKSPACE);
    }

    return workspace;
  }

  public static Workspace create(Project project, String name) {
    name = FileName.removeRestrictedCharacters(name);

    Workspace workspace = getInstance(project, name);
    if (workspace == null) {
      workspace = new Workspace(project, name);
    }

    return workspace;
  }

  private void initialise() {
    try {
      Files.createDirectories(getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    getTestRunWorkspace(getProject());

    /*
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
     */
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
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
