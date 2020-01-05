package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Run extends AbstractRun {
  protected static final String TABLE_NAME = "run";
  private static final String KEY_HOST = "host";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_TRIALS = "trials";
  private static final String KEY_SIMULATOR = "simulator";
  private static final String KEY_STATE = "state";
  private static final String KEY_RESULTS = "results";
  private static final String KEY_EXIT_STATUS = "exit_status";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_ENVIRONMENTS = "environments";

  private static Map<Integer, State> stateMap = new HashMap<>();

  public Run(Project project) {
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

  private String conductor;
  private String trials;
  private String simulator;
  private String host;
  private State state;
  private Integer exitStatus;
  private JSONObject results;
  private JSONObject environments;
  private JSONObject parameters;
  private JSONArray arguments;

  private Run(Conductor conductor, Trial trial, Simulator simulator, Host host) {
    this(conductor.getProject(), UUID.randomUUID(),
      conductor.getId(), trial.getId(), simulator.getId(), host.getId(), State.Created);
  }

  private Run(Project project, UUID id, String name, String conductor, String trials, String simulator, String host, State state) {
    super(project, id, name);
    this.conductor = conductor;
    this.trials = trials;
    this.simulator = simulator;
    this.host = host;
    this.state = state;
  }

  private Run(Project project, UUID id, String conductor, String trials, String simulator, String host, State state) {
    this(project, id, "", conductor, trials, simulator, host, state);
  }

  public static Run getInstance(Project project, String id) {
    final Run[] run = {null};

    handleDatabase(new Run(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createSelect(TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_CONDUCTOR,
          KEY_TRIALS,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE
        ).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          run[0] = new Run(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            resultSet.getString(KEY_CONDUCTOR),
            resultSet.getString(KEY_TRIALS),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE))
          );
        }
      }
    });

    return run[0];
  }

  public Conductor getConductor() {
    return Conductor.getInstance(getProject(), conductor);
  }

  public Host getHost() {
    return Host.getInstance(host);
  }

  public Trial getTrial() {
    return Trial.getInstance(getProject(), trials);
  }

  public Simulator getSimulator() {
    return Simulator.getInstance(getProject(), simulator);
  }

  public State getState() {
    return state;
  }

  public static ArrayList<Run> getList(Project project, Trial parent) {
    ArrayList<Run> list = new ArrayList<>();

    handleDatabase(new Run(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createSelect(TABLE_NAME,
          KEY_ID,
          KEY_CONDUCTOR,
          KEY_TRIALS,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE
        ).where(Sql.Value.equalP(KEY_TRIALS)).toPreparedStatement();
        statement.setString(1, parent.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new Run(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_CONDUCTOR),
            resultSet.getString(KEY_TRIALS),
            resultSet.getString(KEY_SIMULATOR),
            resultSet.getString(KEY_HOST),
            State.valueOf(resultSet.getInt(KEY_STATE))
          ));
        }
      }
    });

    return list;
  }

  public static Run create(ConductorEntity conductorEntity, Simulator simulator, Host host) {
    Run run = new Run(conductorEntity.getConductor(), conductorEntity.getTrial(), simulator, host);
    String conductorId = run.getConductor().getId();
    String trialsId = run.getTrial().getId();
    String simulatorId = run.getSimulator().getId();
    String hostId = run.getHost().getId();
    JSONObject parameters = conductorEntity.getNextRunParameters(simulator);

    handleDatabase(new Run(conductorEntity.getProject()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createInsert(TABLE_NAME,
          KEY_ID,
          KEY_CONDUCTOR,
          KEY_TRIALS,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE,
          KEY_PARAMETERS
        ).toPreparedStatement();
        statement.setString(1, run.getId());
        statement.setString(2, conductorId);
        statement.setString(3, trialsId);
        statement.setString(4, simulatorId);
        statement.setString(5, hostId);
        statement.setInt(6, run.getState().toInt());
        statement.setString(7, parameters.toString());
        statement.execute();
      }
    });

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
        BrowserMessage.addMessage("runUpdated('" + getId() + "')");

        if (state.equals(State.Finished) || state.equals(State.Failed)) {
          for (ConductorEntity entity: ConductorEntity.getList(getTrial())) {
            entity.update();
          }
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

  public int getExitStatus() {
    if (exitStatus == null) {
      exitStatus = Integer.valueOf(getFromDB(KEY_EXIT_STATUS));
    }
    return exitStatus;
  }

  public JSONObject getResults() {
    if (results == null) {
      JSONObject map = new JSONObject(getFromDB(KEY_RESULTS));
      results = map;
    }
    return new JSONObject(results.toString());
  }

  public Object getResult(String key) {
    return getResults().get(key);
  }

  public void putResults(String json) {
    getResults();
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
    JSONObject map = new JSONObject(getFromDB(KEY_RESULTS));
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        results.put(key, valueMap.get(key));
      }

      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_RESULTS + "=? where " + KEY_ID + "=?;");
          statement.setString(1, map.toString());
          statement.setString(2, getId());
          statement.execute();
        }
      });
    }
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

  public JSONObject getParameters() {
    if (parameters == null) {
      parameters = new JSONObject(getFromDB(KEY_PARAMETERS));
    }
    return parameters;
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  public Object setParameter(String key, Object value) {
    getParameters();
    parameters.put(key, value);
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), KEY_PARAMETERS).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, parameters.toString());
        statement.setString(2, getId());
        statement.execute();
      }
    });
    return value;
  }

  public boolean isRunning() {
    return !(state.equals(State.Finished) || state.equals(State.Failed));
  }

  public void start() {
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
                KEY_TRIALS + "," +
                KEY_SIMULATOR + "," +
                KEY_HOST + "," +
                KEY_STATE + "," +
                KEY_ARGUMENTS + " default '[]'," +
                KEY_EXIT_STATUS + " default -1," +
                KEY_ENVIRONMENTS + " default '{}'," +
                KEY_PARAMETERS + " default '{}'," +
                KEY_RESULTS + " default '{}'," +
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

  public EnvironmentsWrapper environments() {
    return new EnvironmentsWrapper(getEnvironments());
  }

  public class EnvironmentsWrapper implements Map<String, Object> {
    Map m;

    public EnvironmentsWrapper(JSONObject o) {
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
      return m.get(k);
    }

    @Override
    public Object put(String s, Object o) {
      return setEnvironment(s, o);
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

  private ArgumentsWrapper argumentsWrapper = new ArgumentsWrapper();
  public ArgumentsWrapper arguments() {
    return argumentsWrapper;
  }

  public class ArgumentsWrapper implements List {

    @Override
    public int size() {
      return getArguments().size();
    }

    @Override
    public boolean isEmpty() {
      return getArguments().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return getArguments().contains(o);
    }

    @Override
    public Iterator iterator() {
      return getArguments().iterator();
    }

    @Override
    public Object[] toArray() {
      return getArguments().toArray();
    }

    @Override
    public boolean add(Object o) {
      addArgument(o);
      return true;
    }

    @Override
    public boolean remove(Object o) {
      return getArguments().remove(o);
    }

    @Override
    public boolean addAll(Collection collection) {
      return getArguments().addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection collection) {
      return getArguments().addAll(i, collection);
    }

    @Override
    public void clear() {
      getArguments().clear();
    }

    @Override
    public Object get(int i) {
      return getArguments().get(i);
    }

    @Override
    public Object set(int i, Object o) {
      return getArguments().set(i, o);
    }

    @Override
    public void add(int i, Object o) {
      getArguments().add(i, o);
    }

    @Override
    public Object remove(int i) {
      return getArguments().remove(i);
    }

    @Override
    public int indexOf(Object o) {
      return getArguments().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      return getArguments().lastIndexOf(o);
    }

    @Override
    public ListIterator listIterator() {
      return getArguments().listIterator();
    }

    @Override
    public ListIterator listIterator(int i) {
      return getArguments().listIterator(i);
    }

    @Override
    public List subList(int i, int i1) {
      return getArguments().subList(i, i1);
    }

    @Override
    public boolean retainAll(Collection collection) {
      return getArguments().retainAll(collection);
    }

    @Override
    public boolean removeAll(Collection collection) {
      return getArguments().removeAll(collection);
    }

    @Override
    public boolean containsAll(Collection collection) {
      return getArguments().containsAll(collection);
    }

    @Override
    public Object[] toArray(Object[] objects) {
      return getArguments().toArray(objects);
    }
  }
}
