package jp.tkms.waffle.data.project.workspace;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.HasNote;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.ProjectData;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.*;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Workspace extends ProjectData implements DataDirectory, PropertyFile, HasNote, Serializable {
  public static final String WORKSPACE = "WORKSPACE";
  public static final String JSON_FILE = WORKSPACE + Constants.EXT_JSON;
  public static final String TESTRUN_WORKSPACE = ".TESTRUN_WORKSPACE";
  public static final String ARCHIVE = ".ARCHIVE";
  public static final String SCRIPT_OUTPUT_FILE = "SCRIPT_OUTPUT.txt";
  public static final String KEY_NOTE_TXT = "NOTE.txt";

  private static final InstanceCache<String, Workspace> instanceCache = new InstanceCache<>();

  private String name = null;

  public Workspace(Project project, String name) {
    super(project);
    this.name = name;
    instanceCache.put(getBaseDirectoryPath(project).resolve(name).toString(), this);
    initialise();
  }

  public String getName() {
    return name;
  }

  @Override
  public Path getPropertyStorePath() {
    return getPath().resolve(JSON_FILE);
  }

  public static Path getBaseDirectoryPath(Project project) {
    return project.getPath().resolve(WORKSPACE);
  }

  public static Workspace getInstance(Project project, String name) {
    if (project != null && name != null && !name.equals("") && Files.exists(getBaseDirectoryPath(project).resolve(name))) {
      Workspace instance = instanceCache.get(getBaseDirectoryPath(project).resolve(name).toString());
      if (instance != null) {
        return instance;
      }
      return new Workspace(project, name);
    }
    return null;
  }

  public static Workspace find(Project project, String key) {
    return getInstance(project, key);
  }

  public static ArrayList<Workspace> getList(Project project) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(project), ChildElementsArrayList.Mode.OnlyNormal, name -> {
      return getInstance(project, name.toString());
    });
  }

  public static ArrayList<Workspace> getHiddenList(Project project) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(project), ChildElementsArrayList.Mode.OnlyHidden, name -> {
      return getInstance(project, name.toString());
    });
  }

  public static Workspace createForce(Project project, String name) {
    Workspace workspace = getInstance(project, name);
    if (workspace == null) {
      workspace = new Workspace(project, name);
    }
    try {
      Path dotSortPath = workspace.getPath().resolve(ChildElementsArrayList.DOT_SORT);
      if (!Files.exists(dotSortPath)) {
        Files.createFile(dotSortPath);
      }
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    return workspace;
  }

  public static Workspace getTestRunWorkspace(Project project) {
    return createForce(project, TESTRUN_WORKSPACE);
  }

  public static Workspace create(Project project, String name) {
    return createForce(project, FileName.generateUniqueFileName(getBaseDirectoryPath(project), name));
  }

  private void initialise() {
    try {
      Files.createDirectories(getPath());
      Files.createDirectories(getPath().resolve(AbstractRun.RUN));
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
  }

  public String getScriptOutput() {
    return getFileContents(SCRIPT_OUTPUT_FILE);
  }

  public void appendScriptOutput(String text) {
    createNewFile(SCRIPT_OUTPUT_FILE);
    appendFileContents(SCRIPT_OUTPUT_FILE, text);
  }

  @Override
  public Path getPath() {
    return getBaseDirectoryPath(getProject()).resolve(name);
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

  public boolean acquireExecutableRunLock(ExecutableRun run) {
  }
}
