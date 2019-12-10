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
  private static final String KEY_ARGUMENTS = "arguments";

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

  public static Run create(Conductor conductor, Trial trial, Simulator simulator, Host host) {
    Run run = new Run(conductor, trial, simulator, host);
    String conductorId = run.getConductor().getId();
    String trialsId = run.getTrial().getId();
    String simulatorId = run.getSimulator().getId();
    String hostId = run.getHost().getId();

    handleDatabase(new Run(conductor.getProject()), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.createInsert(TABLE_NAME,
          KEY_ID,
          KEY_CONDUCTOR,
          KEY_TRIALS,
          KEY_SIMULATOR,
          KEY_HOST,
          KEY_STATE
        ).toPreparedStatement();
        statement.setString(1, run.getId());
        statement.setString(2, conductorId);
        statement.setString(3, trialsId);
        statement.setString(4, simulatorId);
        statement.setString(5, hostId);
        statement.setInt(6, run.getState().toInt());
        statement.execute();
      }
    });

    return run;
  }

  public static Run create(ConductorRun conductorRun, Simulator simulator, Host host) {
    return create(conductorRun.getConductor(), conductorRun.getTrial(), simulator, host);
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
          for (ConductorRun run: ConductorRun.getList(getTrial())) {
            run.update();
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
                KEY_ARGUMENTS + "," +
                KEY_EXIT_STATUS + " default -1," +
                KEY_RESULTS + " default '{}'," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
