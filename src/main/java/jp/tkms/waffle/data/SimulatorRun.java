package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.component.updater.RunStatusUpdater;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.*;

public class SimulatorRun extends AbstractRun {
  protected static final String TABLE_NAME = "simulator_run";
  private static final String KEY_HOST = "host";
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
  private static final String KEY_LOCAL_SHARED = "local_shared";
  public static final String WORKING_DIR = "WORK";
  public static final String KEY_REMOTE_WORKING_DIR = "remote_directory";

  protected SimulatorRun(Project project) {
    super(project);
  }

  private String simulator;
  private String host;
  private State state;
  private Integer exitStatus;
  private Integer restartCount;
  private JSONObject parameters = null;
  private JSONObject results = null;
  private JSONObject environments;
  private JSONArray arguments;
  private JSONArray localSharedList;

  private SimulatorRun(ConductorRun parent, Simulator simulator, Host host, RunNode runNode) {
    this(parent.getProject(), runNode.getUuid(),"",
      simulator.getId(), host.getId(), State.Created, runNode);
  }

  private SimulatorRun(Project project, UUID id, String name, String simulator, String host, State state, RunNode runNode) {
    super(project, id, name, runNode);
    this.simulator = simulator;
    this.host = host;
    this.state = state;
  }

  public static SimulatorRun getInstance(Project project, String id) {
    final SimulatorRun[] run = {null};

    handleDatabase(new SimulatorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_CONDUCTOR,
          KEY_PARENT,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE,
          KEY_RUNNODE
        ).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          run[0] = new SimulatorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE)),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
        }
      }
    });

    return run[0];
  }

  public Host getHost() {
    return Host.getInstance(host);
  }

  public Simulator getSimulator() {
    return Simulator.getInstance(getProject(), simulator);
  }

  public State getState() {
    return state;
  }

  public static ArrayList<SimulatorRun> getList(Project project, ConductorRun parent) {
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
          list.add(new SimulatorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE)),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          ));
        }
      }
    });

    return list;
  }

  public static SimulatorRun create(ConductorRun parent, Simulator simulator, Host host, RunNode runNode) {
    SimulatorRun run = new SimulatorRun(parent, simulator, host, runNode);
    Conductor conductor = parent.getConductor();
    String conductorId = (conductor == null ? "" : conductor.getId());
    String simulatorId = run.getSimulator().getId();
    String hostId = run.getHost().getId();

    ((SimulatorRunNode) runNode).updateState(null, State.Created);

    /*
    JSONObject parameters = ParameterGroup.getRootInstance(simulator).toJSONObject();
    if (copyParameters) {
      for (Map.Entry<String, Object> entry : parent.getParameters().toMap().entrySet()) {
        parameters.put(entry.getKey(), entry.getValue());
      }
    }
     */

    handleDatabase(new SimulatorRun(parent.getProject()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal(KEY_ID, run.getId()),
          Sql.Value.equal(KEY_CONDUCTOR, conductorId),
          Sql.Value.equal(KEY_PARENT, parent.getId()),
          Sql.Value.equal(KEY_SIMULATOR, simulatorId),
          Sql.Value.equal(KEY_HOST, hostId),
          Sql.Value.equal(KEY_STATE, run.getState().ordinal()),
          Sql.Value.equal(KEY_VARIABLES, parent.getVariables().toString()),
          Sql.Value.equal(KEY_RUNNODE, runNode.getId())
        ).execute();
      }
    });

    run.setExitStatus(-1);
    run.setToProperty(KEY_CREATED_AT, ZonedDateTime.now().toEpochSecond());
    run.setToProperty(KEY_SUBMITTED_AT, -1);
    run.setToProperty(KEY_FINISHED_AT, -1);

    new RunStatusUpdater(run);

    try {
      Files.createDirectories(run.getWorkPath());
    } catch (Exception e) {}
    run.parameters = new JSONObject(simulator.getDefaultParameters().toString());
    run.updateParametersStore();

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
  protected Path getPropertyStorePath() {
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
    if (!this.state.equals(state)) {
      ((SimulatorRunNode) getRunNode()).updateState(this.state, state);

      if (
        handleDatabase(this, new Handler() {
          @Override
          void handling(Database db) throws SQLException {
            PreparedStatement statement
              = db.preparedStatement("update " + getTableName() + " set " + KEY_STATE + "=?" + " where id=?;");
            statement.setInt(1, state.ordinal());
            statement.setString(2, getId());
            statement.execute();
          }
        })
      ) {
        this.state = state;
        new RunStatusUpdater(this);

        if (state.equals(State.Finished)
          || state.equals(State.Failed)
          || state.equals((State.Excepted))
          || state.equals(State.Canceled)) {
          setToProperty(KEY_FINISHED_AT, ZonedDateTime.now().toEpochSecond());
          getParent().update(this);
        } else if (state.equals(State.Submitted)) {
          setToProperty(KEY_SUBMITTED_AT, ZonedDateTime.now().toEpochSecond());
        }
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
      restartCount = Integer.valueOf(getStringFromDB(KEY_RESTART_COUNT));
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
    String argumentsJson = this.arguments.toString();

    Path storePath = getDirectoryPath().resolve(KEY_ARGUMENTS + Constants.EXT_JSON);
    try {
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(argumentsJson);
      filewriter.close();
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
      environments = (new JSONObject(getStringFromDB(KEY_ENVIRONMENTS)));
    }
    return environments;
  }

  public Object getEnvironment(String key) {
    return getEnvironments().get(key);
  }

  public Object setEnvironment(String key, Object value) {
    getEnvironments();
    environments.put(key, value);
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Update(db, getTableName(), Sql.Value.equal(KEY_ENVIRONMENTS, environments.toString())).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
    return value;
  }

  protected void updateParametersStore() {
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

    Path storePath = getDirectoryPath().resolve(KEY_PARAMETERS + Constants.EXT_JSON);
    try {
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(parameters.toString(2));
      filewriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getFromParametersStore() {
    Path storePath = getDirectoryPath().resolve(KEY_PARAMETERS + Constants.EXT_JSON);
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
        parameters.put(key, valueMap.get(key));
      }

      updateParametersStore();
    }
  }

  public void putParameter(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putParametersByJson(obj.toString());
  }

  public JSONObject getParameters() {
    if (parameters == null) {
      parameters = new JSONObject(getFromParametersStore());
    }
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  protected void updateResultsStore() {
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

    Path storePath = getDirectoryPath().resolve(KEY_RESULTS + Constants.EXT_JSON);
    try {
      FileWriter filewriter = new FileWriter(storePath.toFile());
      filewriter.write(results.toString(2));
      filewriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getFromResultsStore() {
    Path storePath = getDirectoryPath().resolve(KEY_RESULTS + Constants.EXT_JSON);
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
        results.put(key, valueMap.get(key));
      }

      updateResultsStore();
    }
  }

  public void putResult(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putResultsByJson(obj.toString());
  }

  public JSONObject getResults() {
    if (results == null) {
      results = new JSONObject(getFromResultsStore());
    }
    //return results;
    return new JSONObject(getFromResultsStore());
  }

  public Object getResult(String key) {
    return getResults().get(key);
  }

  @Override
  public boolean isRunning() {
    return (state.equals(State.Created)
      || state.equals(State.Queued)
      || state.equals(State.Submitted)
      || state.equals(State.Running)
    );
  }

  public void start() {
    isStarted = true;
    Job.addRun(this);
  }

  public void restart() {
    setToDB(KEY_RESTART_COUNT, getRestartCount() +1);
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
   */

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
                KEY_SIMULATOR + "," +
                KEY_HOST + "," +
                KEY_STATE + "," +
                KEY_RUNNODE + "," +
                KEY_VARIABLES + " default '{}'," +
                KEY_FINALIZER + " default '[]'," +
                KEY_ARGUMENTS + " default '[]'," +
                KEY_EXIT_STATUS + " default -1," +
                KEY_ENVIRONMENTS + " default '{}'," +
                KEY_RESTART_COUNT + " default 0," +
                KEY_ERROR_NOTE + " default ''," +
                KEY_TIMESTAMP_CREATE + " timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

  public HashMap<String, Object> environmentsWrapper = null;
  public HashMap<String, Object> environments() {
    if (environmentsWrapper == null) {
      environmentsWrapper = new HashMap<>(getEnvironments().toMap()) {
        @Override
        public Object put(String s, Object o) {
          return setEnvironment(s, o);
        }
      };
    }
    return environmentsWrapper;
  }
  public HashMap<String, Object> e() { return environments(); }

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
