package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.*;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.manager.ManagerMaster;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.web.updater.RunStatusUpdater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExecutableRun extends AbstractRun implements DataDirectory, ComputerTask {
  public static final String EXECUTABLE_RUN = "EXECUTABLE_RUN";
  public static final String JSON_FILE = EXECUTABLE_RUN + Constants.EXT_JSON;
  private static final String KEY_EXECUTABLE = "executable";
  private static final String KEY_COMPUTER = "computer";
  private static final String KEY_EXPECTED_NAME = "expected_name";
  private static final String KEY_LOCAL_SHARED = "local_shared";
  private static final String KEY_TASK_ID = "task_id";
  protected static final String KEY_UPDATE_HANDLER = "update_handler";
  protected static final String RESULT_PATH_SEPARATOR = ":";

  //private ProcedureRun parentRun = null;
  private ArchivedExecutable executable = null;
  private Computer computer = null;
  private Computer actualComputer = null;
  private String expectedName = null;
  private Integer exitStatus;

  private State state = null;
  private WrappedJson environments = null;
  private WrappedJsonArray arguments = null;

  private static final InstanceCache<String, ExecutableRun> instanceCache = new InstanceCache<>();

  public ExecutableRun(Workspace workspace, ConductorRun parent, Path path) {
    super(workspace, parent, path);
    instanceCache.put(getLocalPath().toString(), this);
    /*
    if (parent != null) {
      parent.registerChildRun(this);
    }
     */
  }

  public static String debugReport() {
    return ExecutableRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  @Override
  public Path getPropertyStorePath() {
    return this.getPath().resolve(JSON_FILE);
  }

  private static ExecutableRun create(ConductorRun parent, Path path, ArchivedExecutable executable, Computer computer) {
    ExecutableRun run = new ExecutableRun(parent.getWorkspace(), parent, path);
    run.setExecutable(executable);
    run.setComputer(computer);
    run.setActualComputer(computer);
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
    run.putParametersByJson(executable.getDefaultParameters().toString());
    return run;
  }

  public static ExecutableRun create(ConductorRun parent, String expectedName, ArchivedExecutable executable, Computer computer) {
    Path path = FileName.generateUniqueFilePath(parent.getPath().resolve(expectedName));
    ExecutableRun run = create(parent, path, executable, computer);
    run.setExpectedName(expectedName);
    return run;
  }

  public static ExecutableRun create(ProcedureRun parent, String expectedName, Executable executable, Computer computer) {
    Path path = FileName.generateUniqueFilePath(parent.getWorkingDirectory().resolve(expectedName));
    ExecutableRun run = create(parent.getParentConductorRun(), path, StagedExecutable.getInstance(parent.getWorkspace(), executable).getArchivedInstance(), computer);
    run.setExpectedName(expectedName);
    return run;
  }

  public static ExecutableRun getInstance(String localPathString) throws RunNotFoundException {
    ExecutableRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    synchronized (instanceCache) {
      instance = instanceCache.get(localPathString);
      if (instance != null) {
        return instance;
      }

      Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);
      String[] splitPath = localPathString.split(File.separator, 5);
      if (Files.exists(jsonPath) && splitPath.length >= 5 && splitPath[0].equals(Project.PROJECT) && splitPath[2].equals(Workspace.WORKSPACE)) {
        try {
          Project project = Project.getInstance(splitPath[1]);
          Workspace workspace = Workspace.getInstance(project, splitPath[3]);
          WrappedJson jsonObject = new WrappedJson(StringFileUtil.read(jsonPath));
          String parentPath = jsonObject.getString(KEY_PARENT_CONDUCTOR_RUN, null);
          ConductorRun conductorRun = ConductorRun.getInstance(workspace, parentPath);
          instance = new ExecutableRun(workspace, conductorRun, jsonPath.getParent());
        } catch (Exception e) {
          ErrorLogMessage.issue(e);
        }
        return instance;
      }

      throw new RunNotFoundException();
    }
  }

  public static ExecutableRun find(String localPathString) {
    try {
      return getInstance(localPathString);
    } catch (RunNotFoundException e) {
      return null;
    }
  }

  public static String getResultFromFullKey(String fullKey) {
    String[] separatedKey = fullKey.split(RESULT_PATH_SEPARATOR, 2);
    if (separatedKey.length < 2) {
      return null;
    }
    try {
      ExecutableRun run = getInstance(separatedKey[0]);
      return run.getResult(separatedKey[1]).toString();
    } catch (Exception e) {
      return null;
    }
  }

  public String getResultKey(String key) {
    return getLocalPath().toString() + RESULT_PATH_SEPARATOR + key;
  }

  @Override
  public void start() {
    if (started()) {
      return;
    }

    if (getParentConductorRun() != null) {
      getParentConductorRun().registerChildRun(this);
    }

    //getResponsible().registerChildActiveRun(this);
    try {
      putResultsByJson(executable.getDummyResults().toString());
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
    ExecutableRunTask.addRun(this);
  }

  @Override
  public void finish() {
    setState(State.Finalizing);
    /*
    processFinalizers();
    getResponsible().reportFinishedRun(this);
     */

    ManagerMaster.signalFinished(this);

    setState(State.Finished);
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getId() {
    return null;
  }

  public void cancel() {
    if (getTaskId() != null) {
      try {
        ExecutableRunTask job = ExecutableRunTask.getInstance(getTaskId());
        if (job != null) {
          job.cancel();
        }
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public void appendErrorNote(String note) {
    createNewFile(KEY_ERROR_NOTE_TXT);
    updateFileContents(KEY_ERROR_NOTE_TXT, getErrorNote().concat(note).concat("\n"));
  }

  public String getErrorNote() {
    return getFileContents(KEY_ERROR_NOTE_TXT);
  }

  public void putResultDynamically(String key, Object value) {
    putResult(key, value);
    processUpdateHandler(key, value);
  }

  protected void processUpdateHandler(String key, Object value) {
    /*
    String handlerName = getUpdateHandler();
    if (handlerName != null) {
      ProcedureRun handler = ProcedureRun.getInstance(getWorkspace(), handlerName);
      handler.updateResponsible();
      handler.startHandler(ScriptProcessor.ProcedureMode.RESULT_UPDATED, this, new ArrayList<>(Arrays.asList(key, value)));
    }
     */
  }

  public String getUpdateHandler() {
    return getStringFromProperty(KEY_UPDATE_HANDLER, null);
  }

  public void setUpdateHandler(String key) {
    /*
    ProcedureRun handlerRun = createHandler(key);
    setToProperty(KEY_UPDATE_HANDLER, handlerRun.getLocalDirectoryPath().toString());
     */
  }

  protected Path getVariablesStorePath() {
    return getParentConductorRun().getVariablesStorePath();
  }

  public ArchivedExecutable getExecutable() {
    if (executable == null) {
      executable = ArchivedExecutable.getInstance(getWorkspace(), getStringFromProperty(KEY_EXECUTABLE));
    }
    return executable;
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

  public void setTaskId(String taskId) {
    setToProperty(KEY_TASK_ID, taskId);
  }

  @Override
  public Double getRequiredThread() {
    return getExecutable().getRequiredThread();
  }

  @Override
  public Double getRequiredMemory() {
    return getExecutable().getRequiredMemory();
  }

  @Override
  public String getCommand() {
    return getExecutable().getCommand();
  }

  public String getJobId() {
    return getStringFromProperty(KEY_JOB_ID, "");
  }

  public String getTaskId() {
    return getStringFromProperty(KEY_TASK_ID, null);
  }

  public void setRemoteWorkingDirectoryLog(String path) {
    setToProperty(KEY_REMOTE_WORKING_DIR, path);
  }

  public String getRemoteWorkingDirectoryLog() {
    return getStringFromProperty(KEY_REMOTE_WORKING_DIR, "");
  }

  public String getExpectedName() {
    if (expectedName == null) {
      expectedName = getStringFromProperty(KEY_EXPECTED_NAME);
    }
    return expectedName;
  }

  protected void setExecutable(ArchivedExecutable executable) {
    this.executable = executable;
    setToProperty(KEY_EXECUTABLE, executable.getArchiveName());
  }

  protected void setComputer(Computer computer) {
    this.computer = computer;
    setToProperty(KEY_COMPUTER, computer.getName());
  }

  protected void setExpectedName(String expectedName) {
    this.expectedName = expectedName;
    setToProperty(KEY_EXPECTED_NAME, expectedName);
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

  @Override
  public void setState(State state) {
    super.setState(state);

    switch (state) {
      case Submitted:
        setToProperty(KEY_SUBMITTED_AT, DateTime.getCurrentEpoch());
        break;
      case Canceled:
      case Excepted:
      case Failed:
      case Finished:
        setToProperty(KEY_FINISHED_AT, DateTime.getCurrentEpoch());
        //finish();
    }

    if (!State.Created.equals(state)) {
      new RunStatusUpdater(this);
    }
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
    return this.getPath().resolve(Executable.BASE).toAbsolutePath();
  }

  @Override
  public Path getBinPath() {
    return getExecutable().getBaseDirectory().toAbsolutePath();
  }

  @Override
  public Path getRemoteBinPath() {
    return getExecutable().getLocalPath();
  }

  @Override
  public void specializedPreProcess(AbstractSubmitter submitter) {
    for (String extractorName : getExecutable().getExtractorNameList()) {
      ScriptProcessor.getProcessor(getExecutable().getScriptProcessorName()).processExtractor(submitter, this, extractorName);
    }
  }

  @Override
  public void specializedPostProcess(AbstractSubmitter submitter, AbstractTask job) throws OccurredExceptionsException, RunNotFoundException {
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
  }

  public static ExecutableRun createTestRun(Executable executable, Computer computer) {
    Workspace workspace = Workspace.getTestRunWorkspace(executable.getProject());
    ArchivedExecutable archivedExecutable = StagedExecutable.getInstance(workspace, executable, true).getArchivedInstance();
    //ProcedureRun procedureRun = ProcedureRun.getTestRunProcedureRun(archivedExecutable);
    ConductorRun conductorRun = ConductorRun.getTestRunConductorRun(archivedExecutable);
    return create(conductorRun, Main.DATE_FORMAT.format(System.currentTimeMillis()), archivedExecutable, computer);
  }

  public ArrayList<Object> getArguments() {
    if (arguments == null) {
      Path storePath = this.getPath().resolve(ARGUMENTS_JSON_FILE);
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

    Path storePath = this.getPath().resolve(ARGUMENTS_JSON_FILE);
    try {
      JsonWriter.writeValue(storePath, this.arguments);
      /*
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(argumentsJson);
      filewriter.close();
       */
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    if (! Files.exists(this.getPath())) {
      try {
        Files.createDirectories(this.getPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (parameters == null) {
      parameters = new WrappedJson();
    }

    Path storePath = getParametersStorePath();
    try {
      JsonWriter.writeValue(storePath, parameters);
      /*
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(parameters.toString(2));
      filewriter.close();
       */
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Path getParametersStorePath() {
    return this.getPath().resolve(PARAMETERS_JSON_FILE);
  }

  public long getParametersStoreSize() {
    return getParametersStorePath().toFile().length();
  }

  private String getFromParametersStore() {
    Path storePath = getParametersStorePath();
    String json = "{}";
    if (Files.exists(storePath)) {
      try {
        json = new String(Files.readAllBytes(storePath));
      } catch (IOException e) {
        e.printStackTrace();
      }
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
    if (! Files.exists(this.getPath())) {
      try {
        Files.createDirectories(this.getPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (results == null) {
      results = new WrappedJson();
    }

    Path storePath = getResultsStorePath();
    try {
      JsonWriter.writeValue(storePath, results);
      /*
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(results.toString(2));
      filewriter.close();
       */
    } catch (IOException e) {
      e.printStackTrace();
    }

    ManagerMaster.signalUpdated(this);
  }

  private Path getResultsStorePath() {
    return this.getPath().resolve(RESULTS_JSON_FILE);
  }

  public long getResultsStoreSize() {
    return getResultsStorePath().toFile().length();
  }

  private String getFromResultsStore() {
    Path storePath = getResultsStorePath();
    String json = "{}";
    if (Files.exists(storePath)) {
      try {
        json = new String(Files.readAllBytes(storePath));
      } catch (IOException e) {
        e.printStackTrace();
      }
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

  public String getStdout() {
    if (Files.exists(getPath().resolve(Constants.STDOUT_FILE))) {
      return getFileContents(Constants.STDOUT_FILE);
    }
    return null;
  }

  public String getStderr() {
    if (Files.exists(getPath().resolve(Constants.STDERR_FILE))) {
      return getFileContents(Constants.STDERR_FILE);
    }
    return null;
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
}
