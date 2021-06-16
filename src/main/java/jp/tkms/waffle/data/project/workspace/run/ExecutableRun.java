package jp.tkms.waffle.data.project.workspace.run;

import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.ExecutableRunTaskStore;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.*;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import jp.tkms.waffle.web.updater.RunStatusUpdater;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExecutableRun extends AbstractRun implements ComputerTask {
  public static final String EXECUTABLE_RUN = "EXECUTABLE_RUN";
  public static final String JSON_FILE = EXECUTABLE_RUN + Constants.EXT_JSON;
  private static final String KEY_EXECUTABLE = "executable";
  private static final String KEY_COMPUTER = "computer";
  private static final String KEY_EXPECTED_NAME = "expected_name";
  private static final String KEY_LOCAL_SHARED = "local_shared";
  private static final String KEY_TASK_ID = "task_id";
  protected static final String KEY_UPDATE_HANDLER = "update_handler";

  //private ProcedureRun parentRun = null;
  private ArchivedExecutable executable = null;
  private Computer computer = null;
  private Computer actualComputer = null;
  private String expectedName = null;
  private Integer exitStatus;

  private JSONArray localSharedList;
  private State state = null;
  private JSONObject environments = null;
  private JSONArray arguments = null;

  private static final InstanceCache<String, ExecutableRun> instanceCache = new InstanceCache<>();

  public ExecutableRun(Workspace workspace, RunCapsule parent, Path path) {
    super(workspace, parent, path);
    instanceCache.put(getLocalDirectoryPath().toString(), this);
  }

  public static String debugReport() {
    return ExecutableRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ExecutableRun create(ProcedureRun parent, String expectedName, ArchivedExecutable executable, Computer computer) {
    RunCapsule capsule = RunCapsule.create(parent, parent.generateUniqueFileName(expectedName));
    String name = capsule.generateUniqueFileName(expectedName);
    ExecutableRun run = new ExecutableRun(capsule.getWorkspace(), capsule, capsule.getDirectoryPath().resolve(name));
    run.setParent(capsule);
    run.setExecutable(executable);
    run.setComputer(computer);
    run.setActualComputer(computer);
    run.setExpectedName(expectedName);
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
    run.getParent().registerChildRun(run);
    run.updateResponsible();
    run.putParametersByJson(executable.getDefaultParameters().toString());
    return run;
  }

  public static ExecutableRun create(ProcedureRun parent, String expectedName, Executable executable, Computer computer) {
    return create(parent, expectedName, StagedExecutable.getInstance(parent.getWorkspace(), executable).getArchivedInstance(), computer);
  }

  public static ExecutableRun getInstance(String localPathString) throws RunNotFoundException {
    ExecutableRun instance = instanceCache.get(localPathString);
    if (instance != null) {
      return instance;
    }

    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);
    String[] splitPath = localPathString.split(File.separator, 5);
    if (Files.exists(jsonPath) && splitPath.length == 5 && splitPath[0].equals(Project.PROJECT) && splitPath[2].equals(Workspace.WORKSPACE)) {
      try {
        Project project = Project.getInstance(splitPath[1]);
        Workspace workspace = Workspace.getInstance(project, splitPath[3]);
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        String parentPath = jsonObject.getString(KEY_PARENT_RUN);
        RunCapsule capsule = RunCapsule.getInstance(workspace, parentPath);
        instance = new ExecutableRun(workspace, capsule, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      return instance;
    }
    throw new RunNotFoundException();
  }

  public static ExecutableRun find(String localPathString) {
    try {
      return getInstance(localPathString);
    } catch (RunNotFoundException e) {
      return null;
    }
  }

  @Override
  public void start() {
    if (started()) {
      return;
    }
    getResponsible().registerChildActiveRun(this);
    try {
      putResultsByJson(executable.getDummyResults().toString());
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
    ExecutableRunJob.addRun(this);
  }

  @Override
  public void finish() {
    setState(State.Finalizing);
    processFinalizers();
    getResponsible().reportFinishedRun(this);
    setState(State.Finished);
  }

  public void cancel() {
    if (getTaskId() != null) {
      try {
        ExecutableRunJob job = ExecutableRunJob.getInstance(getTaskId());
        if (job != null) {
          job.cancel();
        }
      } catch (RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public void putResultDynamically(String key, Object value) {
    putResult(key, value);
    processUpdateHandler(key, value);
  }

  protected void processUpdateHandler(String key, Object value) {
    String handlerName = getUpdateHandler();
    if (handlerName != null) {
      ProcedureRun handler = ProcedureRun.getInstance(getWorkspace(), handlerName);
      handler.updateResponsible();
      handler.startHandler(ScriptProcessor.ProcedureMode.RESULT_UPDATED, this, new ArrayList<>(Arrays.asList(key, value)));
    }
  }

  public String getUpdateHandler() {
    return getStringFromProperty(KEY_UPDATE_HANDLER, null);
  }

  public void setUpdateHandler(String key) {
    ProcedureRun handlerRun = createHandler(key);
    setToProperty(KEY_UPDATE_HANDLER, handlerRun.getLocalDirectoryPath().toString());
  }

  @Override
  protected Path getVariablesStorePath() {
    return getParent().getVariablesStorePath();
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

  public JSONArray getLocalSharedList() {
    if (localSharedList == null) {
      try {
        localSharedList = getArrayFromProperty(KEY_LOCAL_SHARED);
      } catch (Exception e) {}
      if (localSharedList == null) {
        putNewArrayToProperty(KEY_LOCAL_SHARED);
        localSharedList = new JSONArray();
      }
    }
    return localSharedList;
  }

  public void makeLocalShared(String key, String remote) {
    localSharedList = getLocalSharedList();
    JSONArray entry = new JSONArray();
    entry.put(FileName.removeRestrictedCharacters(key));
    entry.put(remote);
    localSharedList.put(entry);
    setToProperty(KEY_LOCAL_SHARED, this.localSharedList);
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

    new RunStatusUpdater(this);
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
    return getDirectoryPath().resolve(Executable.BASE).toAbsolutePath();
  }

  @Override
  public Path getBinPath() {
    return getExecutable().getBaseDirectory().toAbsolutePath();
  }

  @Override
  public Path getRemoteBinPath() {
    return getExecutable().getLocalDirectoryPath();
  }

  @Override
  public void specializedPreProcess(AbstractSubmitter submitter) {
    for (String extractorName : getExecutable().getExtractorNameList()) {
      ScriptProcessor.getProcessor(getExecutable().getScriptProcessorName()).processExtractor(submitter, this, extractorName);
    }
  }

  @Override
  public void specializedPostProcess(AbstractSubmitter submitter, AbstractJob job) throws OccurredExceptionsException, RunNotFoundException {
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
    ProcedureRun procedureRun = ProcedureRun.getTestRunProcedureRun(archivedExecutable);
    return create(procedureRun, Main.DATE_FORMAT.format(System.currentTimeMillis()), archivedExecutable, computer);
  }

  public ArrayList<Object> getArguments() {
    if (arguments == null) {
      Path storePath = getDirectoryPath().resolve(ARGUMENTS_JSON_FILE);
      String json = "[]";
      if (Files.exists(storePath)) {
        try {
          json = new String(Files.readAllBytes(storePath));
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        setArguments(new ArrayList<>());
      }
      arguments = new JSONArray(json);
    }
    return new ArrayList<>(arguments.toList());
  }

  public void setArguments(ArrayList<Object> arguments) {
    this.arguments = new JSONArray(arguments);
    //String argumentsJson = this.arguments.toString();

    Path storePath = getDirectoryPath().resolve(ARGUMENTS_JSON_FILE);
    try {
      JSONWriter.writeValue(storePath, this.arguments);
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

  public JSONObject getEnvironments() {
    if (environments == null) {
      environments = getJSONObjectFromProperty(KEY_ENVIRONMENTS, new JSONObject());
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

  protected void updateParametersStore(JSONObject parameters) {
    //protected void updateParametersStore() {
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (parameters == null) {
      parameters = new JSONObject();
    }

    Path storePath = getParametersStorePath();
    try {
      JSONWriter.writeValue(storePath, parameters);
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
    return getDirectoryPath().resolve(PARAMETERS_JSON_FILE);
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
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      //BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    //JSONObject map = new JSONObject(getFromDB(KEY_PARAMETERS));
    JSONObject map = new JSONObject(getFromParametersStore());
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        //parameters.put(key, valueMap.get(key));
      }

      updateParametersStore(map);
    }
  }

  public void putParameter(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putParametersByJson(obj.toString());
  }

  public JSONObject getParameters() {
    //if (parameters == null) {
    JSONObject parameters = new JSONObject(getFromParametersStore());
    //}
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  protected void updateResultsStore(JSONObject results) {
    //protected void updateResultsStore() {
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (results == null) {
      results = new JSONObject();
    }

    Path storePath = getResultsStorePath();
    try {
      JSONWriter.writeValue(storePath, results);
      /*
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(results.toString(2));
      filewriter.close();
       */
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Path getResultsStorePath() {
    return getDirectoryPath().resolve(RESULTS_JSON_FILE);
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
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      //BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      e.printStackTrace();
    }
    //JSONObject map = new JSONObject(getFromDB(KEY_PARAMETERS));
    JSONObject map = new JSONObject(getFromResultsStore());
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        //results.put(key, valueMap.get(key));
      }

      updateResultsStore(map);
    }
  }

  public void putResult(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putResultsByJson(obj.toString());
  }

  public JSONObject getResults() {
    //if (results == null) {
    //JSONObject results = new JSONObject(getFromResultsStore());
    //}
    //return results;
    return new JSONObject(getFromResultsStore());
  }

  public Object getResult(String key) {
    return getResults().get(key);
  }

  public HashMap<Object, Object> environmentsWrapper = null;
  public HashMap environments() {
    if (environmentsWrapper == null) {
      environmentsWrapper = new HashMap<>(getEnvironments().toMap()) {
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
      parametersWrapper = new HashMap<Object, Object>(getParameters().toMap()) {
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
