package jp.tkms.waffle.data;

import jp.tkms.waffle.component.updater.RunStatusUpdater;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SimulatorRun extends AbstractRun {
  protected static final String TABLE_NAME = "simulator_run";
  private static final String KEY_HOST = "host";
  private static final String KEY_SIMULATOR = "simulator";
  private static final String KEY_STATE = "state";
  private static final String KEY_RESTART_COUNT = "restart";
  private static final String KEY_EXIT_STATUS = "exit_status";
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_ENVIRONMENTS = "environments";

  private static Map<Integer, State> stateMap = new HashMap<>();

  protected SimulatorRun(Project project) {
    super(project);
  }

  public enum State {
    Created(0), Queued(1), Submitted(2), Running(3), Finished(4), Failed(5);

    private final int id;

    State(final int id) {
      this.id = id;
      stateMap.put(id, this);
    }

    int toInt() { return id; }

    static State valueOf(int i) {
      return stateMap.get(i);
    }
  }

  private String simulator;
  private String host;
  private State state;
  private Integer exitStatus;
  private Integer restartCount;
  private JSONObject environments;
  private JSONArray arguments;

  private SimulatorRun(Conductor conductor, Simulator simulator, Host host) {
    this(conductor.getProject(), UUID.randomUUID(),
      conductor.getId(), simulator.getId(), host.getId(), State.Created);
  }

  private SimulatorRun(Project project, UUID id, String name, String simulator, String host, State state) {
    super(project, id, name);
    this.simulator = simulator;
    this.host = host;
    this.state = state;
  }

  private SimulatorRun(Project project, UUID id, String simulator, String host, State state) {
    this(project, id, "", simulator, host, state);
  }

  public static SimulatorRun getInstance(Project project, String id) {
    final SimulatorRun[] run = {null};

    handleDatabase(new SimulatorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createSelect(TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_CONDUCTOR,
          KEY_PARENT,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE
        ).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          run[0] = new SimulatorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE))
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
        PreparedStatement statement = db.createSelect(TABLE_NAME,
          KEY_ID,
          KEY_CONDUCTOR,
          KEY_PARENT,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE
        ).where(Sql.Value.equalP(KEY_PARENT)).toPreparedStatement();
        statement.setString(1, parent.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new SimulatorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_CONDUCTOR),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE))
          ));
        }
      }
    });

    return list;
  }

  public static SimulatorRun create(ConductorRun parent, Simulator simulator, Host host) {
    SimulatorRun run = new SimulatorRun(parent.getConductor(), simulator, host);
    String conductorId = run.getConductor().getId();
    String simulatorId = run.getSimulator().getId();
    String hostId = run.getHost().getId();
    JSONObject parameters = parent.getNextRunParameters(simulator);

    handleDatabase(new SimulatorRun(parent.getProject()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createInsert(TABLE_NAME,
          KEY_ID,
          KEY_CONDUCTOR,
          KEY_PARENT,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE,
          KEY_PARAMETERS
        ).toPreparedStatement();
        statement.setString(1, run.getId());
        statement.setString(2, conductorId);
        statement.setString(3, parent.getId());
        statement.setString(4, simulatorId);
        statement.setString(5, hostId);
        statement.setInt(6, run.getState().toInt());
        statement.setString(7, parameters.toString());
        statement.execute();
      }
    });

    new RunStatusUpdater(run);

    return run;
  }

  public void setState(State state) {
    if (!this.state.equals(state)) {
      if (
        handleDatabase(this, new Handler() {
          @Override
          void handling(Database db) throws SQLException {
            PreparedStatement statement
              = db.preparedStatement("update " + getTableName() + " set " + KEY_STATE + "=?" + " where id=?;");
            statement.setInt(1, state.toInt());
            statement.setString(2, getId());
            statement.execute();
          }
        })
      ) {
        this.state = state;
        new RunStatusUpdater(this);

        if (state.equals(State.Finished) || state.equals(State.Failed)) {
          getParent().update(this);
        }
      }
    }
  }

  public void setExitStatus(int exitStatus) {
    if (
      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_EXIT_STATUS + "=?" + " where id=?;");
          statement.setInt(1, exitStatus);
          statement.setString(2, getId());
          statement.execute();
        }
      })
    ) {
      this.exitStatus = exitStatus;
    }
  }

  public int getRestartCount() {
    if (restartCount == null) {
      restartCount = Integer.valueOf(getFromDB(KEY_RESTART_COUNT));
    }
    return restartCount;
  }

  public int getExitStatus() {
    if (exitStatus == null) {
      exitStatus = Integer.valueOf(getFromDB(KEY_EXIT_STATUS));
    }
    return exitStatus;
  }

  public ArrayList<Object> getArguments() {
    if (arguments == null) {
      arguments = new JSONArray(getFromDB(KEY_ARGUMENTS));
    }
    return new ArrayList<>(arguments.toList());
  }

  public void setArguments(ArrayList<Object> arguments) {
    this.arguments = new JSONArray(arguments);
    String argumentsJson = this.arguments.toString();

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_ARGUMENTS + "=? where " + KEY_ID + "=?;");
        statement.setString(1, argumentsJson);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public void addArgument(Object o) {
    ArrayList<Object> arguments = getArguments();
    arguments.add(o);
    setArguments(arguments);
  }

  public JSONObject getEnvironments() {
    if (environments == null) {
      environments = (new JSONObject(getFromDB(KEY_ENVIRONMENTS)));
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
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_ENVIRONMENTS).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, environments.toString());
        statement.setString(2, getId());
        statement.execute();
      }
    });
    return value;
  }

  @Override
  public boolean isRunning() {
    return !(state.equals(State.Finished) || state.equals(State.Failed));
  }

  public void start() {
    Job.addRun(this);
  }

  public void restart() {
    setIntToDB(KEY_RESTART_COUNT, getRestartCount() +1);
    setExitStatus(-1);
    setState(State.Created);
    Job.addRun(this);
  }

  public void recheck() {
    setExitStatus(-1);
    setState(State.Running);
    Job.addRun(this);
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
                KEY_SIMULATOR + "," +
                KEY_HOST + "," +
                KEY_STATE + "," +
                KEY_PARAMETERS + " default '{}'," +
                KEY_FINALIZER + " default '[]'," +
                KEY_ARGUMENTS + " default '[]'," +
                KEY_EXIT_STATUS + " default -1," +
                KEY_ENVIRONMENTS + " default '{}'," +
                KEY_RESTART_COUNT + " default 0," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

  public ParametersWrapper parameters() {
    return new ParametersWrapper(getParameters());
  }

  public class ParametersWrapper implements Map<String, Object> {
    Map m;

    public ParametersWrapper(JSONObject o) {
      m = o.toMap();
    }

    @Override
    public int size() {
      return m.size();
    }

    @Override
    public boolean isEmpty() {
      return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
      return m.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
      return m.containsValue(o);
    }

    @Override
    public Object get(Object k) {
      Object o = getParameters().get(k.toString());
      if (o instanceof JSONObject) {
        return new ParametersWrapper((JSONObject) o);
      }
      return o;
    }

    @Override
    public Object put(String s, Object o) {
      return setParameter(s, o);
    }

    @Override
    public Object remove(Object o) {
      return m.remove(o);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
      m.putAll(map);
    }

    @Override
    public void clear() {
      m.clear();
    }

    @Override
    public Set<String> keySet() {
      return m.keySet();
    }

    @Override
    public Collection<Object> values() {
      return m.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return m.entrySet();
    }
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
}
