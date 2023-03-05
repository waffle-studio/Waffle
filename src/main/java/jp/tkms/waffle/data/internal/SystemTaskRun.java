package jp.tkms.waffle.data.internal;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.util.*;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.communicator.AbstractSubmitter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class SystemTaskRun implements ComputerTask, DataDirectory, PropertyFile {
  public static final String SYSTEM_TASK_RUN = "SYSTEM_TASK_RUN";
  public static final String JSON_FILE = SYSTEM_TASK_RUN + Constants.EXT_JSON;
  private static final String KEY_BIN_PATH = "bin_path";
  protected static final String KEY_COMMAND = "command";
  private static final String KEY_REQUIRED_THREAD = "required_thread";
  private static final String KEY_REQUIRED_MEMORY = "required_memory";
  private static final String KEY_STATE = "state";
  private static final String KEY_CLASS = "class";

  private Path path;
  private Computer computer = null;
  private Computer actualComputer = null;
  private Integer exitStatus;

  private State state = null;
  private WrappedJson environments = null;
  private WrappedJsonArray arguments = null;

  private static final InstanceCache<String, SystemTaskRun> instanceCache = new InstanceCache<>();

  public SystemTaskRun(Path path) {
    this.path = path;
    instanceCache.put(getLocalPath().toString(), this);
    setToProperty(KEY_CLASS, getClass().getName());
  }

  public static String debugReport() {
    return SystemTaskRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  WrappedJson jsonObject = null;
  @Override
  public WrappedJson getPropertyStoreCache() {
    return jsonObject;
  }

  @Override
  public void setPropertyStoreCache(WrappedJson cache) {
    jsonObject = cache;
  }

  @Override
  public Path getPropertyStorePath() {
    return getPath().resolve(JSON_FILE);
  }

  public static SystemTaskRun create(Path path, Path binPath, String command, int requiredThread, int requiredMemory, Computer computer) {
    SystemTaskRun run = new SystemTaskRun(path);
    run.setCommand(command);
    run.setBinPath(binPath);
    run.setRequiredThread(requiredThread);
    run.setRequiredMemory(requiredMemory);
    run.setComputer(computer);
    run.setState(State.Created);
    run.setToProperty(KEY_EXIT_STATUS, -1);
    run.setToProperty(KEY_CREATED_AT, DateTime.getCurrentEpoch());
    run.setToProperty(KEY_SUBMITTED_AT, DateTime.getEmptyEpoch());
    run.setToProperty(KEY_FINISHED_AT, DateTime.getEmptyEpoch());
    try {
      Files.createDirectories(run.getBasePath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return run;
  }

  public static SystemTaskRun getInstance(String localPathString) throws RunNotFoundException {
    SystemTaskRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);
    if (Files.exists(jsonPath)) {
      try {
        Class<SystemTaskRun> clazz = SystemTaskRun.class;
        try {
          WrappedJson jsonObject = new WrappedJson(StringFileUtil.read(jsonPath));
          String className = jsonObject.getString(KEY_CLASS, null);
          clazz = (Class<SystemTaskRun>) Class.forName(className);
        } catch (ClassNotFoundException e) {
          ErrorLogMessage.issue(e);
        }

        Constructor<SystemTaskRun> constructor;
        try {
          constructor = clazz.getConstructor(Path.class);
        } catch (SecurityException | NoSuchMethodException e) {
          ErrorLogMessage.issue(e);
          return null;
        }

        try {
          instance = constructor.newInstance(jsonPath.getParent());
        } catch (IllegalArgumentException | ReflectiveOperationException e) {
          ErrorLogMessage.issue(e);
          return null;
        }
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      return instance;
    }
    throw new RunNotFoundException();
  }

  public void start() {
    SystemTask.addRun(this);
  }

  public void finish() {
    setState(State.Finished);
  }

  @Override
  public State getState() {
    return State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
  }

  public Computer getComputer() {
    if (computer == null) {
      computer = Computer.getInstance(getStringFromProperty(KEY_COMPUTER));
    }
    return computer;
  }

  public void setJobId(String jobId) {
    setToProperty(KEY_JOB_ID, jobId);
  }

  public void setRequiredThread(double requiredThread) {
    setToProperty(KEY_REQUIRED_THREAD, requiredThread);
  }

  public void setRequiredMemory(double requiredMemory) {
    setToProperty(KEY_REQUIRED_MEMORY, requiredMemory);
  }

  @Override
  public Double getRequiredThread() {
    return getDoubleFromProperty(KEY_REQUIRED_THREAD);
  }

  @Override
  public Double getRequiredMemory() {
    return getDoubleFromProperty(KEY_REQUIRED_MEMORY);
  }

  public void setCommand(String command) {
    setToProperty(KEY_COMMAND, command);
  }

  @Override
  public String getCommand() {
    return getStringFromProperty(KEY_COMMAND);
  }

  @Override
  public boolean isRunning() {
    State state = getState();
    return (state.equals(State.Created)
      || state.equals(State.Prepared)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
      || state.equals(State.Finalizing)
    );
  }

  public String getJobId() {
    return getStringFromProperty(KEY_JOB_ID, "");
  }

  public void setRemoteWorkingDirectoryLog(String path) {
    setToProperty(KEY_REMOTE_WORKING_DIR, path);
  }

  public void setActualComputer(Computer computer) {
    this.actualComputer = computer;
    setToProperty(KEY_ACTUAL_COMPUTER, computer.getName());
  }

  public Computer getActualComputer() {
    if (actualComputer == null) {
      actualComputer = Computer.getInstance(getStringFromProperty(KEY_ACTUAL_COMPUTER));
    }
    return actualComputer;
  }

  public String getRemoteWorkingDirectoryLog() {
    return getStringFromProperty(KEY_REMOTE_WORKING_DIR, "");
  }

  protected void setComputer(Computer computer) {
    this.computer = computer;
    setToProperty(KEY_COMPUTER, computer.getName());
  }

  public void setExitStatus(int exitStatus) {
    setToProperty(KEY_EXIT_STATUS, exitStatus);
    this.exitStatus = exitStatus;
  }

  public int getExitStatus() {
    if (exitStatus == null) {
      exitStatus = getIntFromProperty(KEY_EXIT_STATUS, -2);
    }
    return exitStatus;
  }

  public void setState(State state) {
    setToProperty(KEY_STATE, state.ordinal());

    switch (state) {
      case Submitted:
        setToProperty(KEY_SUBMITTED_AT, DateTime.getCurrentEpoch());
        break;
      case Aborted:
      case Excepted:
      case Failed:
      case Finished:
        setToProperty(KEY_FINISHED_AT, DateTime.getCurrentEpoch());
        //finish();
    }

    //new RunStatusUpdater(this);
  }

  public DateTime getCreatedDateTime() {
    return new DateTime(getLongFromProperty(KEY_CREATED_AT, DateTime.getEmptyEpoch()));
  }

  public DateTime getSubmittedDateTime() {
    return new DateTime(getLongFromProperty(KEY_SUBMITTED_AT, DateTime.getEmptyEpoch()));
  }

  public DateTime getFinishedDateTime() {
    return new DateTime(getLongFromProperty(KEY_FINISHED_AT, DateTime.getEmptyEpoch()));
  }

  public Path getBasePath() {
    return getPath().resolve(Executable.BASE).toAbsolutePath();
  }

  public void setBinPath(Path binPath) {
    setToProperty(KEY_BIN_PATH, binPath == null ? null : binPath.toString());
  }

  @Override
  public Path getBinPath() {
    String path = getStringFromProperty(KEY_BIN_PATH);
    return path == null ? null : Paths.get(path);
  }

  @Override
  public Path getRemoteBinPath() {
    Path path = getBinPath();
    return path == null ? null : Constants.WORK_DIR.relativize(path.toAbsolutePath());
  }

  @Override
  public void specializedPreProcess(AbstractSubmitter submitter) {
    /*
    for (String extractorName : getExecutable().getExtractorNameList()) {
      ScriptProcessor.getProcessor(getExecutable().getScriptProcessorName()).processExtractor(submitter, this, extractorName);
    }
     */
  }

  @Override
  public void specializedPostProcess(AbstractSubmitter submitter, AbstractTask job) throws OccurredExceptionsException, RunNotFoundException {
    /*
    boolean isNoException = true;
    for (String collectorName : getExecutable().getCollectorNameList()) {
      try {
        ScriptProcessor.getProcessor(getExecutable().getScriptProcessorName()).processCollector(submitter, this, collectorName);
      } catch (Exception | Error e) {
        isNoException = false;
        job.setState(State.Excepted);
        appendErrorNote(LogMessage.getStackTrace(e));
        WarnLogMessage.issue(e);
      }
    }
    if (!isNoException) {
      throw new OccurredExceptionsException();
    }
     */
  }

  public ArrayList<Object> getArguments() {
    if (arguments == null) {
      Path storePath = getPath().resolve(ARGUMENTS_JSON_FILE);
      String json = "[]";
      if (Files.exists(storePath)) {
        json = StringFileUtil.read(storePath);
      } else {
        setArguments(new ArrayList<>());
      }
      arguments = new WrappedJsonArray(json);
    }
    return new ArrayList<>(arguments);
  }

  public void setArguments(ArrayList<Object> arguments) {
    this.arguments = new WrappedJsonArray(arguments);
    //String argumentsJson = this.arguments.toString();

    Path storePath = getPath().resolve(ARGUMENTS_JSON_FILE);
    this.arguments.writeMinimalFile(storePath);
  }

  public void addArgument(Object o) {
    ArrayList<Object> arguments = getArguments();
    arguments.add(o);
    setArguments(arguments);
  }

  public WrappedJson getEnvironments() {
    if (environments == null) {
      environments = getObjectFromProperty(KEY_ENVIRONMENTS, new WrappedJson());
    }
    return environments;
  }

  @Override
  public void appendErrorNote(String note) {

  }

  public Object getEnvironment(String key) {
    return getEnvironments().get(key);
  }

  public Object putEnvironment(String key, Object value) {
    environments = getEnvironments();
    environments.put(key, value);
    setToProperty(KEY_ENVIRONMENTS, environments);
    return environments;
  }

  protected void updateParametersStore(WrappedJson parameters) {
    //protected void updateParametersStore() {
    if (! Files.exists(getPath())) {
      try {
        Files.createDirectories(getPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (parameters == null) {
      parameters = new WrappedJson();
    }

    Path storePath = getParametersStorePath();
    parameters.writeMinimalFile(storePath);
  }

  private Path getParametersStorePath() {
    return getPath().resolve(PARAMETERS_JSON_FILE);
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

  public void putParametersByJson(String json) {
    getParameters(); // init.
    WrappedJson valueMap = null;
    try {
      valueMap = new WrappedJson(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      //BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    //WrappedJson map = new WrappedJson(getFromDB(KEY_PARAMETERS));
    WrappedJson map = new WrappedJson(getFromParametersStore());
    if (valueMap != null) {
      map.merge(valueMap);
      updateParametersStore(map);
    }
  }

  public void putParameter(String key, Object value) {
    WrappedJson obj = new WrappedJson();
    obj.put(key, value);
    putParametersByJson(obj.toString());
  }

  public WrappedJson getParameters() {
    //if (parameters == null) {
    WrappedJson parameters = new WrappedJson(getFromParametersStore());
    //}
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  protected void updateResultsStore(WrappedJson results) {
    //protected void updateResultsStore() {
    if (! Files.exists(getPath())) {
      try {
        Files.createDirectories(getPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (results == null) {
      results = new WrappedJson();
    }

    Path storePath = getResultsStorePath();
    results.writeMinimalFile(storePath);
  }

  private Path getResultsStorePath() {
    return getPath().resolve(RESULTS_JSON_FILE);
  }

  public long getResultsStoreSize() {
    return getResultsStorePath().toFile().length();
  }

  private String getFromResultsStore() {
    Path storePath = getResultsStorePath();
    String json = "{}";
    if (Files.exists(storePath)) {
      json = StringFileUtil.read(storePath);
    }
    return json;
  }

  public void putResultsByJson(String json) {
    getResults(); // init.
    WrappedJson valueMap = null;
    try {
      valueMap = new WrappedJson(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      //BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    //WrappedJson map = new WrappedJson(getFromDB(KEY_PARAMETERS));
    WrappedJson map = new WrappedJson(getFromResultsStore());
    if (valueMap != null) {
      map.merge(valueMap);
      updateResultsStore(map);
    }
  }

  public void putResult(String key, Object value) {
    WrappedJson obj = new WrappedJson();
    obj.put(key, value);
    putResultsByJson(obj.toString());
  }

  public WrappedJson getResults() {
    //if (results == null) {
    //WrappedJson results = new WrappedJson(getFromResultsStore());
    //}
    //return results;
    return new WrappedJson(getFromResultsStore());
  }

  public Object getResult(String key) {
    return getResults().get(key);
  }

  public HashMap<Object, Object> environmentsWrapper = null;
  public HashMap environments() {
    if (environmentsWrapper == null) {
      environmentsWrapper = new HashMap<>(getEnvironments()) {
        @Override
        public Object get(Object key) {
          return getEnvironment(key.toString());
        }

        @Override
        public Object put(Object key, Object value) {
          super.put(key, value);
          putEnvironment(key.toString(), value);
          return value;
        }
      };
    }
    return environmentsWrapper;
  }
  public HashMap e() { return environments(); }

  private ArrayList<Object> argumentsWrapper = null;
  public ArrayList<Object> arguments() {
    if (argumentsWrapper == null) {
      argumentsWrapper = new ArrayList<>(getArguments()) {
        @Override
        public boolean add(Object o) {
          addArgument(o);
          return true;
        }
      };
    }
    return argumentsWrapper;
  }
  public ArrayList<Object> a() { return arguments(); }

  private HashMap<Object, Object> parametersWrapper = null;
  public HashMap parameters() {
    if (parametersWrapper == null) {
      parametersWrapper = new HashMap<Object, Object>(getParameters()) {
        @Override
        public Object get(Object key) {
          return getParameter(key.toString());
        }

        @Override
        public Object put(Object key, Object value) {
          super.put(key, value);
          putParameter(key.toString(), value);
          return value;
        }

        @Override
        public String toString() {
          return getParameters().toString();
        }
      };
    }

    return parametersWrapper;
  }
  public HashMap p() { return parameters(); }

  @Override
  public Path getPath() {
    return path;
  }
}
