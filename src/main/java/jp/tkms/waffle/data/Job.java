package jp.tkms.waffle.data;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Future;

public class Job extends Data {
  private static final String TABLE_NAME = "job";
  private static final String KEY_PROJECT = "project";
  private static final String KEY_HOST = "host";
  private static final String KEY_JOB_ID = "job_id";
  private static final String KEY_STATE = "state";
  private static final String KEY_ERROR_COUNT = "error_count";

  private static final HashMap<String, Job> instanceMap = new HashMap<>();

  private Project project = null;
  private Host host = null;
  private SimulatorRun run = null;
  private String jobId = null;
  private State state = null;
  private Integer errorCount = null;

  public Job(UUID id) {
    super(id, "");
  }

  public Job() { }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Job getInstance(String id) {
    final Job[] job = {null};

    job[0] = instanceMap.get(id);
    if (job[0] != null) {
      return job[0];
    }

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          job[0] = new Job(
            UUID.fromString(resultSet.getString("id"))
          );
        }
      }
    });

    instanceMap.put(id, job[0]);

    return job[0];
  }

  public static ArrayList<Future<Job>> getList() {
    ArrayList<Future<Job>> list = new ArrayList<>();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          String id = resultSet.getString("id");
          list.add(
            Main.threadPool.submit(() -> {
                return new Job(UUID.fromString(id));
              }
            )
          );
        }
      }
    });

    return list;
  }

  public static ArrayList<Future<Job>> getList(int offset, int limit) {
    ArrayList<Future<Job>> list = new ArrayList<>();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + " limit " + offset + "," + limit + ";");
        while (resultSet.next()) {
          String id = resultSet.getString("id");
          list.add(
            Main.threadPool.submit(() -> {
                return new Job(UUID.fromString(id));
              }
            )
          );
        }
      }
    });

    return list;
  }

  public static ArrayList<Job> getList(Host host) {
    ArrayList<Job> list = new ArrayList<>();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select id from " + TABLE_NAME + " where " + KEY_HOST + "=?;");
        statement.setString(1, host.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new Job(
            UUID.fromString(resultSet.getString("id"))
          ));
        }
      }
    });

    return list;
  }

  public static int getNum() {
    final int[] num = {0};

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select count(*) as num from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          num[0] = resultSet.getInt("num");
        }
      }
    });

    return num[0];
  }

  public static void addRun(SimulatorRun run) {
    String hostId = run.getHost().getId();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet
          = new Sql.Select(db, TABLE_NAME, "count(*) as count").where(Sql.Value.equal(KEY_ID, run.getId())).executeQuery();
        int count = 0;
        while (resultSet.next()) {
          count = resultSet.getInt("count");
        }
        if (count <= 0) {
          new Sql.Insert(db, TABLE_NAME,
            Sql.Value.equal(KEY_ID, run.getId()),
            Sql.Value.equal(KEY_PROJECT, run.getProject().getId()),
            Sql.Value.equal(KEY_HOST, hostId),
            Sql.Value.equal(KEY_STATE, run.getState().ordinal())
            ).execute();
        }
      }
    });
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");

    getInstance(run.getId()); //load cache
  }

  public void cancel() throws RunNotFoundException {
    if (getRun().isRunning()) {
      setState(State.Cancel);
    }
  }

  public void remove() {

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Delete(db, getTableName()).where(Sql.Value.equal(KEY_ID, getId())).execute();
      }
    });
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");

    instanceMap.remove(getId());
  }

  public void setJobId(String jobId) throws RunNotFoundException {
    setToDB(KEY_JOB_ID, jobId);
    this.jobId = jobId;
    getRun().setJobId(jobId);
  }

  public void setState(State state) throws RunNotFoundException {
    setToDB(KEY_STATE, state.ordinal());
    this.state = state;
    if (getRun() != null) {
      getRun().setState(state);
    }
  }

  public void incrementErrorCount() {
    if (
      handleDatabase(new Job(), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_ERROR_COUNT + "=" + KEY_ERROR_COUNT + " +1 where id=?;");
          statement.setString(1, getId());
          statement.execute();
        }
      })
    ) {
      errorCount = null;
    }
  }

  public Path getLocation() {
    Path path = Paths.get( TABLE_NAME + File.separator + name + '_' + shortId );
    return path;
  }

  public Project getProject() {
    if (project == null) {
      project = Project.getInstance(getStringFromDB(KEY_PROJECT));
    }
    return project;
  }

  public Host getHost() {
    if (host == null) {
      host = Host.getInstance(getStringFromDB(KEY_HOST));
    }
    return host;
  }

  public SimulatorRun getRun() throws RunNotFoundException {
    if (run == null) {
      run = SimulatorRun.getInstance(getProject(), getId());
    }
    return run;
  }

  public String getJobId() {
    if (jobId == null) {
      jobId = getStringFromDB(KEY_JOB_ID);
    }
    return jobId;
  }

  public State getState() {
    if (state == null) {
      state = State.valueOf(getIntFromDB(KEY_STATE));
    }
    return state;
  }

  public int getErrorCount() {
    if (errorCount == null) {
      errorCount = Integer.valueOf(getStringFromDB(KEY_ERROR_COUNT));
    }
    return errorCount;
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
              db.execute("create table " + TABLE_NAME + "(id," +
                KEY_PROJECT + "," +
                KEY_HOST + "," +
                KEY_JOB_ID + " default ''," +
                KEY_ERROR_COUNT + " default 0," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          },
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              new Sql.AlterTable(db, TABLE_NAME, Sql.AlterTable.withDefault(KEY_STATE, String.valueOf(State.Created.ordinal()))).execute();
            }
          }
        ));
      }
    };
  }
}
