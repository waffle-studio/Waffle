package jp.tkms.waffle.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Run extends ProjectData{
  protected static final String TABLE_NAME = "run";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_HOST = "host";
  private static final String KEY_TRIALS = "trials";
  private static final String KEY_SIMULATOR = "simulator";
  private static final String KEY_STATE = "state";

  private static Map<Integer, State> stateMap = new HashMap<>();
  public enum State {
    Created(1), Submitted(2), Running(3), Finished(4), Failed(5);

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

  private Run(Conductor conductor, Trials trials, Simulator simulator, Host host) {
    this(conductor.getProject(), UUID.randomUUID(),
      conductor.getId(), trials.getId(), simulator.getId(), host.getId(), State.Created);
  }

  private Run(Project project, UUID id, String conductor, String trials, String simulator, String host, State state) {
    super(project, id, "");
    this.conductor = conductor;
    this.trials = trials;
    this.simulator = simulator;
    this.host = host;
    this.state = state;
  }

  public static Run getInstance(Project project, String id) {
    Run run = null;
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement = db.createSelect(TABLE_NAME,
        KEY_ID,
        KEY_CONDUCTOR,
        KEY_TRIALS,
        KEY_SIMULATOR,
        KEY_HOST,
        KEY_STATE
      ).where(Sql.Value.state(KEY_ID)).preparedStatement();
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        run = new Run(
          project,
          UUID.fromString(resultSet.getString(KEY_ID)),
          resultSet.getString(KEY_CONDUCTOR),
          resultSet.getString(KEY_TRIALS),
          resultSet.getString(KEY_SIMULATOR),
          resultSet.getString(KEY_HOST),
          State.valueOf(resultSet.getInt(KEY_STATE))
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return run;
  }

  public Conductor getConductor() {
    return Conductor.getInstance(project, conductor);
  }

  public Host getHost() {
    return Host.getInstance(host);
  }

  public Trials getTrials() {
    return Trials.getInstance(project, trials);
  }

  public Simulator getSimulator() {
    return Simulator.getInstance(project, simulator);
  }

  public State getState() {
    return state;
  }

  public static ArrayList<Run> getList(Project project, Trials parent) {
    ArrayList<Run> list = new ArrayList<>();
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement = db.createSelect(TABLE_NAME,
        KEY_ID,
        KEY_CONDUCTOR,
        KEY_TRIALS,
        KEY_SIMULATOR,
        KEY_HOST,
        KEY_STATE
        ).where(Sql.Value.state(KEY_TRIALS)).preparedStatement();
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

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static Run create(Conductor conductor, Trials trials, Simulator simulator, Host host) {
    Run run = new Run(conductor, trials, simulator, host);


    try {
      Database db = getWorkDB(conductor.getProject(), workDatabaseUpdater);
      PreparedStatement statement = db.createInsert(TABLE_NAME,
        KEY_ID,
        KEY_CONDUCTOR,
        KEY_TRIALS,
        KEY_SIMULATOR,
        KEY_HOST,
        KEY_STATE
      ).preparedStatement();
      statement.setString(1, run.getId());
      statement.setString(2, run.getConductor().getId());
      statement.setString(3, run.getTrials().getId());
      statement.setString(4, run.getSimulator().getId());
      statement.setString(5, run.getHost().getId());
      statement.setInt(6, run.getState().toInt());
      statement.execute();
      db.commit();
      db.close();


      //Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return run;
  }

  public void setState(State state) {
    try {
      Database db = getWorkDB();
      PreparedStatement statement
        = db.preparedStatement("update " + getTableName() + " set " + KEY_STATE + "=?" + " where id=?;");
      statement.setInt(1, state.toInt());
      statement.setString(2, getId());
      statement.execute();
      db.commit();
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void start() {
    Job.addRun(this);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return null;
  }

  @Override
  protected DatabaseUpdater getWorkDatabaseUpdater() {
    return null;
  }

  private static DatabaseUpdater workDatabaseUpdater = new DatabaseUpdater() {
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
              "id," +
              KEY_CONDUCTOR + "," +
              KEY_TRIALS + "," +
              KEY_SIMULATOR + "," +
              KEY_HOST + "," +
              KEY_STATE + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
