package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.web.updater.RunStatusUpdater;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.JSONWriter;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimulatorRun extends AbstractRun {
  protected static final String TABLE_NAME = "simulator_run";
  private static final String KEY_COMPUTER = "computer";
  private static final String KEY_SIMULATOR = "simulator";
  private static final String KEY_RESTART_COUNT = "restart";
  private static final String KEY_EXIT_STATUS = "exit_status";
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_ENVIRONMENTS = "environments";
  protected static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_RESULTS = "results";
  private static final String KEY_CREATED_AT = "created_at";
  private static final String KEY_SUBMITTED_AT = "submitted_at";
  private static final String KEY_FINISHED_AT = "finished_at";
  private static final String KEY_RUN = "run";
  private static final String KEY_ENTRY = "entry";
  private static final String KEY_JOB_ID = "job_id";
  private static final String KEY_LOCAL_SHARED = "local_shared";
  public static final String WORKING_DIR = "WORK";
  public static final String KEY_REMOTE_WORKING_DIR = "remote_directory";
  private static final String KEY_ACTUAL_COMPUTER = "actual_computer";

  private String simulator;
  private String computer;
  private State state;
  private Integer exitStatus;
  private Integer restartCount;
  private JSONObject environments;
  private JSONArray arguments;
  private JSONArray localSharedList;

  private static final ConcurrentHashMap<String, SimulatorRun> instanceMap = new ConcurrentHashMap<>();

  private SimulatorRun(Project project, UUID id, Path path) {
    super(project, id, path);
  }

  public static SimulatorRun getInstance(Project project, String id) throws RunNotFoundException {
    SimulatorRun run = null;

    if (id != null) {
      run = instanceMap.get(id);
      if (run != null)  {
        return run;
      }


      RunNode runNode = RunNode.getInstance(project, id);

      if (runNode != null && runNode instanceof SimulatorRunNode) {
        run = new SimulatorRun(project, runNode.getUuid(), runNode.getDirectoryPath());
      }
    }

    if (run == null) {
      throw new RunNotFoundException();
    }

    return run;
  }

  public Computer getComputer() {
    if (computer == null) {
      computer = getStringFromProperty(KEY_COMPUTER);
    }
    return Computer.getInstance(computer);
  }

  public void setActualHost(Computer computer) {
    setToProperty(KEY_ACTUAL_COMPUTER, computer.getName());
  }

  public Computer getActualHost() {
    return Computer.getInstance(getStringFromProperty(KEY_ACTUAL_COMPUTER, getComputer().getName()));
  }

  public Executable getSimulator() {
    if (simulator == null) {
      simulator = getStringFromProperty(KEY_SIMULATOR);
    }
    return Executable.getInstance(getProject(), simulator);
  }

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
    }
    return state;
  }

  /*
  public static ArrayList<SimulatorRun> getList(Project project, Actor parent) {
    ArrayList<SimulatorRun> list = new ArrayList<>();

    handleDatabase(new SimulatorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_PARENT,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE,
          KEY_RUNNODE
        ).where(Sql.Value.equal(KEY_PARENT, parent.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          SimulatorRun run = instanceMap.get(resultSet.getString(KEY_ID));
          if (run != null) {
            list.add(run);
            continue;
          }
          run = new SimulatorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
          instanceMap.put(run.getId(), run);
          list.add(run);
        }
      }
    });

    return list;
  }

  public static int getNumberOfRunning(Project project, Actor parent) {
    final int[] count = new int[1];

    handleDatabase(new SimulatorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, "count(*) as count")
          .where(
            Sql.Value.and(
              Sql.Value.equal(KEY_PARENT, parent.getId()), Sql.Value.lessThan(KEY_STATE, State.Finished.ordinal())
            )).executeQuery();
        while (resultSet.next()) {
          count[0] = resultSet.getInt("count");
        }
      }
    });

    return count[0];
  }
   */

  public static SimulatorRun create(RunNode runNode, ActorRun parent, Executable executable, Computer computer) {
    SimulatorRun run = new SimulatorRun(parent.getProject(), runNode.getUuid(), runNode.getDirectoryPath());

    Conductor conductor = parent.getActorGroup();
    String actorGroupName = (conductor == null ? "" : conductor.getName());
    String simulatorName = executable.getName();
    String computerName = computer.getName();
    JSONArray callstack = parent.getCallstack();
    callstack.put("##");

    ((SimulatorRunNode) runNode).updateState(null, State.Created);

    /*
    JSONObject parameters = ParameterGroup.getRootInstance(simulator).toJSONObject();
    if (copyParameters) {
      for (Map.Entry<String, Object> entry : parent.getParameters().toMap().entrySet()) {
        parameters.put(entry.getKey(), entry.getValue());
      }
    }
     */

    run.setToProperty(KEY_RUNNODE, runNode.getId());
    run.setToProperty(KEY_OWNER, parent.getOwner().getId());
    run.setToProperty(KEY_ACTOR_GROUP, actorGroupName);
    run.setToProperty(KEY_PARENT, parent.getId());
    run.setToProperty(KEY_RESPONSIBLE_ACTOR, parent.getId());
    run.setToProperty(KEY_SIMULATOR, simulatorName);
    run.setToProperty(KEY_COMPUTER, computerName);
    run.setToProperty(KEY_ACTUAL_COMPUTER, computerName);
    run.setToProperty(KEY_STATE, run.getState().ordinal());
    //run.setToProperty(KEY_VARIABLES, parent.getVariables().toString());
    run.putVariablesByJson(parent.getVariables().toString());
    run.setToProperty(KEY_CALLSTACK, callstack.toString());

    run.setExitStatus(-1);
    run.setToProperty(KEY_CREATED_AT, ZonedDateTime.now().toEpochSecond());
    run.setToProperty(KEY_SUBMITTED_AT, -1);
    run.setToProperty(KEY_FINISHED_AT, -1);

    new RunStatusUpdater(run);

    try {
      Files.createDirectories(run.getWorkPath());
    } catch (Exception e) {}
    run.putParametersByJson(executable.getDefaultParameters().toString());

    return run;
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

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_RUN + Constants.EXT_JSON);
  }

  public Path getWorkPath() {
    return getDirectoryPath().resolve(WORKING_DIR).toAbsolutePath();
  }

  @Override
  public void appendErrorNote(String note) {
    super.appendErrorNote(note);
  }

  public void setState(State state) {
    if (!this.getState().equals(state)) {
      switch (state) {
        case Finished:
        case Failed:
        case Excepted:
        case Canceled:
          setToProperty(KEY_FINISHED_AT, ZonedDateTime.now().toEpochSecond());
          break;
        case Submitted:
          setToProperty(KEY_SUBMITTED_AT, ZonedDateTime.now().toEpochSecond());
      }

      State prev = getState();
      this.state = state;
      setToProperty(KEY_STATE, state.ordinal());

      try {
        ((SimulatorRunNode) getRunNode()).updateState(prev, state);
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      new RunStatusUpdater(this);

      if (!isRunning()) {
        finish();
      }
    }
  }

  public void setRemoteWorkingDirectoryLog(String path) {
    setToProperty(KEY_REMOTE_WORKING_DIR, path);
  }

  public String getRemoteWorkingDirectoryLog() {
    return getStringFromProperty(KEY_REMOTE_WORKING_DIR);
  }

  public int getRestartCount() {
    if (restartCount == null) {
      restartCount = getIntFromProperty(KEY_RESTART_COUNT, 0);
    }
    return restartCount;
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

  public void setJobId(String jobId) {
    setToProperty(KEY_JOB_ID, jobId);
  }

  public String getJobId() {
    return getStringFromProperty(KEY_JOB_ID, "");
  }

  public ArrayList<Object> getArguments() {
    if (arguments == null) {
      Path storePath = getDirectoryPath().resolve(KEY_ARGUMENTS + Constants.EXT_JSON);
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

    Path storePath = getDirectoryPath().resolve(KEY_ARGUMENTS + Constants.EXT_JSON);
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
    return getDirectoryPath().resolve(KEY_PARAMETERS + Constants.EXT_JSON);
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
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
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
    return getDirectoryPath().resolve(KEY_RESULTS + Constants.EXT_JSON);
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
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
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

  @Override
  public boolean isRunning() {
    State state = getState();
    return (state.equals(State.Created)
      || state.equals(State.Prepared)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
    );
  }

  public void start() {
    super.start();
    instanceMap.put(getId(), this);
    putVariablesByJson(getParentActor().getVariables().toString());
    isStarted = true;
    Job.addRun(this);
  }

  @Override
  public void finish() {
    super.finish();
    instanceMap.remove(getId());
  }

  public void restart() {
    setToProperty(KEY_RESTART_COUNT, getRestartCount() +1);
    setExitStatus(-1);
    setState(State.Created);
    Job.addRun(this);
  }

  public void recheck() {
    setExitStatus(-1);
    setState(State.Running);
    Job.addRun(this);
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

  /*
  public ArrayList<Object> getStageInList() {
    if (stageInList == null) {
      stageInList = getArrayFromProperty(KEY_STAGE_IN);
      if (stageInList == null) {
        putNewArrayToProperty(KEY_STAGE_IN);
        stageInList = new JSONArray();
      }
    }
    return new ArrayList<>(stageInList.toList());
  }

  public void setStageInList(ArrayList<Object> stageInList) {
    this.stageInList = new JSONArray(stageInList);
    setToProperty(KEY_STAGE_IN, this.stageInList);
  }

  public void stageIn(String name, String remote) {
    ArrayList<Object> stageInList = getStageInList();
    JSONArray entry = new JSONArray();
    entry.put(name);
    entry.put(remote);
    stageInList.add(entry);
    setStageInList(stageInList);
  }

  public void stageOut(String name, String remote) {
    try {
      AbstractSubmitter submitter = AbstractSubmitter.getInstance(getHost()).connect();
      submitter.connect(true);
      submitter.stageOut(this, name, remote);
      submitter.close();
    } catch (Exception e) {}
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
      @Override
      String tableName() {
        return TABLE_NAME;
      }

      @Override
      ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("create table " + TABLE_NAME + "(" +
                KEY_ID + "," +
                KEY_NAME + "," +
                KEY_CONDUCTOR + "," +
                KEY_PARENT + "," +
                KEY_RESPONSIBLE_ACTOR + "," +
                KEY_SIMULATOR + "," +
                KEY_HOST + "," +
                KEY_STATE + "," +
                KEY_RUNNODE + "," +
                KEY_PARENT_RUNNODE + "," +
                KEY_VARIABLES + " default '{}'," +
                KEY_FINALIZER + " default '[]'," +
                KEY_CALLSTACK + " default '[]'," +
                KEY_ARGUMENTS + " default '[]'," +
                KEY_EXIT_STATUS + " default -1," +
                KEY_ENVIRONMENTS + " default '{}'," +
                KEY_RESTART_COUNT + " default 0," +
                KEY_TIMESTAMP_CREATE + " timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
   */

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
