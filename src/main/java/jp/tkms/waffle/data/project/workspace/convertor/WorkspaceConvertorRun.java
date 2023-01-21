package jp.tkms.waffle.data.project.workspace.convertor;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.convertor.WorkspaceConvertor;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.WorkspaceData;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.project.workspace.conductor.StagedConductor;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.manager.ManagerMaster;
import jp.tkms.waffle.script.ScriptProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorkspaceConvertorRun extends WorkspaceData implements PropertyFile, DataDirectory {
  private static final String CONVERTOR = "CONVERTOR";
  public static final String PARAMETERS_JSON_FILE = "PARAMETERS" + Constants.EXT_JSON;
  public static final String KEY_ERROR_NOTE_TXT = "ERROR_NOTE.txt";
  protected static final String KEY_STATE = "state";
  protected static final String KEY_STARTED = "started";

  private State state = null;
  private Path path;
  private WorkspaceConvertor convertor;

  public WorkspaceConvertorRun(Workspace workspace, WorkspaceConvertor convertor, Path path) {
    super(workspace);
    this.convertor = convertor;
    this.path = path;
  }

  public Path getPath() {
    return path;
  }

  public WorkspaceConvertor getConvertor() {
    return convertor;
  }

  private static WorkspaceConvertorRun create(Workspace workspace, WorkspaceConvertor convertor, Path path) {
    WorkspaceConvertorRun instance = new WorkspaceConvertorRun(workspace, convertor, path);
    instance.setState(State.Created);
    instance.loadDefaultParameters();
    return instance;
  }

  public static WorkspaceConvertorRun create(Workspace workspace, WorkspaceConvertor convertor, String expectedName) {
    Path path = FileName.generateUniqueFilePath(getBaseDirectoryPath(workspace).resolve(expectedName));
    return create(workspace, convertor, path);
  }

  public void start(boolean async) {
    if (async) {
      Main.systemThreadPool.submit(new Runnable() {
        @Override
        public void run() {
          start();
        }
      });
    } else {
      start();
    }
  }

  public void start() {
    started();
    setState(State.Running);
    try {
      ScriptProcessor.getProcessor(getConvertor().getScriptPath()).processConvertor(this);
    } catch (Exception | Error e) {
      setState(State.Excepted);
      appendErrorNote(LogMessage.getStackTrace(e));
      WarnLogMessage.issue(e);
    }
    setState(State.Finished);
  }

  protected Path getParametersStorePath() {
    return this.getPath().resolve(PARAMETERS_JSON_FILE);
  }

  protected void updateParametersStore(WrappedJson variables) {
    //protected void updateParametersStore() {
    if (! Files.exists(this.getPath())) {
      try {
        Files.createDirectories(this.getPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (variables == null) {
      variables = new WrappedJson();
    }

    Path storePath = getParametersStorePath();
    variables.writePrettyFile(storePath);
  }

  public long getParametersStoreSize() {
    return getParametersStorePath().toFile().length();
  }

  private String getFromParametersStore() {
    Path storePath = getParametersStorePath();
    String json = "{}";
    if (Files.exists(storePath)) {
      json = StringFileUtil.read(storePath);
    }
    return json;
  }

  public void putParameters(WrappedJson valueMap) {
    getParameters(); // init.
    WrappedJson map = new WrappedJson(getFromParametersStore());
    if (valueMap != null) {
      map.merge(valueMap);
      updateParametersStore(map);
    }
  }

  public void putParametersByJson(String json) {
    if (json == null) { return; }

    try {
      putParameters(new WrappedJson(json));
    } catch (Exception e) {
      System.err.println(json);
      WarnLogMessage.issue(e);
    }
  }

  public void putParameter(String key, Object value) {
    WrappedJson json = new WrappedJson();
    json.put(key, value);
    putParameters(json);
  }

  public WrappedJson getParameters() {
    return new WrappedJson(getFromParametersStore()).withUpdateHandler((v)->{ updateParametersStore(v); });
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  void loadDefaultParameters() {
    if (convertor != null) {
      putParameters(convertor.getDefaultParameters());
    }
  }

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
    }
    return state;
  }

  protected void setState(State state) {
    this.state = state;
    setToProperty(KEY_STATE, state.ordinal());
  }

  public boolean isRunning() {
    State state = getState();
    return (state.equals(State.Created)
      || state.equals(State.Prepared)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
      || state.equals(State.Finalizing)
    );
  }

  protected boolean started() {
    boolean currentState = isStarted();
    setToProperty(KEY_STARTED, true);
    return currentState;
  }

  public boolean isStarted() {
    return getBooleanFromProperty(KEY_STARTED, false);
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getPath().resolve(CONVERTOR);
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
