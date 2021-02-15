package jp.tkms.waffle.data.internal;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.SystemTaskJob;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.project.workspace.run.RunCapsule;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class SystemTaskRun implements ComputerTask, DataDirectory, PropertyFile {
  public static final String EXECUTABLE_RUN = "EXECUTABLE_RUN";
  public static final String JSON_FILE = EXECUTABLE_RUN + Constants.EXT_JSON;
  public static final String PARAMETERS_JSON_FILE = "PARAMETERS" + Constants.EXT_JSON;
  public static final String RESULTS_JSON_FILE = "RESULTS" + Constants.EXT_JSON;
  public static final String ARGUMENTS_JSON_FILE = "ARGUMENTS" + Constants.EXT_JSON;
  public static final String KEY_ENVIRONMENTS = "environments";
  private static final String KEY_EXECUTABLE = "executable";
  private static final String KEY_COMPUTER = "computer";
  private static final String KEY_ACTUAL_COMPUTER = "actual_computer";
  private static final String KEY_EXPECTED_NAME = "expected_name";
  public static final String KEY_REMOTE_WORKING_DIR = "remote_directory";
  private static final String KEY_BIN_PATH = "bin_path";
  protected static final String KEY_CREATED_AT = "created_at";
  protected static final String KEY_SUBMITTED_AT = "submitted_at";
  protected static final String KEY_FINISHED_AT = "finished_at";
  protected static final String KEY_EXIT_STATUS = "exit_status";
  protected static final String KEY_JOB_ID = "job_id";
  protected static final String KEY_COMMAND = "command";
  private static final String KEY_REQUIRED_THREAD = "required_thread";
  private static final String KEY_REQUIRED_MEMORY = "required_memory";
  private static final String KEY_STATE = "state";
  private static final String BASE = "BASE";

  private Path path;
  private Computer computer = null;
  private Integer exitStatus;

  private State state = null;
  private JSONObject environments = null;
  private JSONArray arguments = null;

  private static final InstanceCache<String, SystemTaskRun> instanceCache = new InstanceCache<>();

  public SystemTaskRun(Path path) {
    this.path = path;
    instanceCache.put(getLocalDirectoryPath().toString(), this);
  }

  public static String debugReport() {
    return SystemTaskRun.class.getSimpleName() + ": instanceCacheSize=" + instanceCache.size();
  }

  @Override
  public JSONObject getPropertyStoreCache() {
    return null;
  }

  @Override
  public void setPropertyStoreCache(JSONObject cache) {

  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
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
        instance = new SystemTaskRun(jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      return instance;
    }
    throw new RunNotFoundException();
  }

  public void start() {
    SystemTaskJob.addRun(this);
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

  public void setRequiredThread(int requiredThread) {
    setToProperty(KEY_REQUIRED_THREAD, requiredThread);
  }

  public void setRequiredMemory(int requiredMemory) {
    setToProperty(KEY_REQUIRED_MEMORY, requiredMemory);
  }

  @Override
  public double getRequiredThread() {
    return getIntFromProperty(KEY_REQUIRED_THREAD);
  }

  @Override
  public double getRequiredMemory() {
    return getIntFromProperty(KEY_REQUIRED_MEMORY);
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

  @Override
  public Computer getActualComputer() {
    return getComputer();
  }

  public void setActualComputer(Computer computer) {
    setToProperty(KEY_COMPUTER, computer.getName());
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
    return getDirectoryPath().resolve(Executable.BASE).toAbsolutePath();
  }

  public void setBinPath(Path binPath) {
    setToProperty(KEY_BIN_PATH, binPath.toString());
  }

  @Override
  public Path getBinPath() {
    return Paths.get(getStringFromProperty(KEY_BIN_PATH));
  }

  @Override
  public Path getRemoteBinPath() {
    return getLocalDirectoryPath().resolve(BASE);
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
  public void specializedPostProcess(AbstractSubmitter submitter, AbstractJob job) throws OccurredExceptionsException, RunNotFoundException {
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

  @Override
  public Path getDirectoryPath() {
    return path;
  }
}
