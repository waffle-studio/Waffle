package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.JSONWriter;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.plaf.ComponentUI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class ExecutableRun extends AbstractRun {
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
  private static final String KEY_STATE = "state";
  public static final String KEY_REMOTE_WORKING_DIR = "remote_directory";
  private static final String KEY_LOCAL_SHARED = "local_shared";
  private static final String KEY_CREATED_AT = "created_at";
  private static final String KEY_SUBMITTED_AT = "submitted_at";
  private static final String KEY_FINISHED_AT = "finished_at";
  private static final String KEY_EXIT_STATUS = "exit_status";
  private static final String KEY_JOB_ID = "job_id";

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

  public ExecutableRun(Workspace workspace, ProcedureRun parent, Path path) {
    super(workspace, parent, path);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ExecutableRun create(ProcedureRun parent, String expectedName, ArchivedExecutable executable, Computer computer) {
    String name = parent.generateUniqueFileName(expectedName);
    ExecutableRun run = new ExecutableRun(parent.getWorkspace(), parent, parent.getDirectoryPath().resolve(name));
    run.setParent(parent);
    run.setExecutable(executable);
    run.setComputer(computer);
    run.setExpectedName(expectedName);
    return run;
  }

  public static ExecutableRun getInstance(String localPathString) {
    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);
    String[] splitPath = localPathString.split(File.separator, 5);
    if (Files.exists(jsonPath) && splitPath.length == 5 && splitPath[0].equals(Project.PROJECT) && splitPath[2].equals(Workspace.WORKSPACE)) {
      try {
        Project project = Project.getInstance(splitPath[1]);
        Workspace workspace = Workspace.getInstance(project, splitPath[3]);
        JSONObject jsonObject = new JSONObject(Files.readString(jsonPath));
        String parentPath = jsonObject.getString(KEY_PARENT_RUN);
        ProcedureRun parent = ProcedureRun.getInstance(workspace, parentPath);
        return new ExecutableRun(workspace, parent, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }
    return null;
  }

  public void start() {
    putParametersByJson(executable.getDefaultParameters().toString());
    putResultsByJson(executable.getDummyResults().toString());
    Job.addRun(this);
  }

  public void finish() {

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

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
    }
    return state;
  }

  public void setJobId(String jobId) {
    setToProperty(KEY_JOB_ID, jobId);
  }

  public String getJobId() {
    return getStringFromProperty(KEY_JOB_ID, "");
  }

  public void setRemoteWorkingDirectoryLog(String path) {
    setToProperty(KEY_REMOTE_WORKING_DIR, path);
  }

  public String getRemoteWorkingDirectoryLog() {
    return getStringFromProperty(KEY_REMOTE_WORKING_DIR);
  }

  public String getExpectedName() {
    if (expectedName == null) {
      expectedName = getStringFromProperty(KEY_EXPECTED_NAME);
    }
    return expectedName;
  }

  private void setExecutable(ArchivedExecutable executable) {
    this.executable = executable;
    setToProperty(KEY_EXECUTABLE, executable.getArchiveName());
  }

  private void setComputer(Computer computer) {
    this.computer = computer;
    setToProperty(KEY_COMPUTER, computer.getName());
  }

  private void setExpectedName(String expectedName) {
    this.expectedName = expectedName;
    setToProperty(KEY_EXPECTED_NAME, expectedName);
  }

  public void setExitStatus(int exitStatus) {
    setToProperty(KEY_EXIT_STATUS, exitStatus);
    this.exitStatus = exitStatus;
  }

  public int getExitStatus() {
    if (exitStatus == null) {
      exitStatus = getIntFromProperty(KEY_EXIT_STATUS);
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
    entry.put(key);
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

  public void setState(State state) {
    //TODO:
  }
  public boolean isRunning() {
    State state = getState();
    return (state.equals(State.Created)
      || state.equals(State.Prepared)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
    );
  }

  public DateTime getCreatedDateTime() {
    return new DateTime(getLongFromProperty(KEY_CREATED_AT));
  }

  public DateTime getSubmittedDateTime() {
    return new DateTime(getLongFromProperty(KEY_SUBMITTED_AT));
  }

  public DateTime getFinishedDateTime() {
    return new DateTime(getLongFromProperty(KEY_FINISHED_AT));
  }

  public Path getBasePath() {
    return getDirectoryPath().resolve(Executable.BASE).toAbsolutePath();
  }

  public static ExecutableRun createTestRun(Executable executable, Computer computer) {
    Workspace workspace = Workspace.getTestRunWorkspace(executable.getProject());
    ArchivedExecutable archivedExecutable = StagedExecutable.getInstance(workspace, executable).getEntity();
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
    JSONObject results = new JSONObject(getFromResultsStore());
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
