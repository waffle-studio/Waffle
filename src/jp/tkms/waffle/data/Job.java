package jp.tkms.waffle.data;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Job extends Data {
  private static final String TABLE_NAME = "job";
  private static final String KEY_PROJECT = "projrct";
  private static final String KEY_HOST = "host";
  private static final String KEY_JOB_ID = "job_id";

  private Project project = null;
  private Host host = null;
  private Run run = null;
  private String jobId = null;

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

    return job[0];
  }

  public static ArrayList<Job> getList() {
    ArrayList<Job> list = new ArrayList<>();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          list.add(new Job(
            UUID.fromString(resultSet.getString("id"))
          ));
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

  public static void addRun(Run run) {
    String hostId = run.getHost().getId();

    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("insert into " + TABLE_NAME + "(id,"
          + KEY_PROJECT + ","
          + KEY_HOST
          + ") values(?,?,?);");
        statement.setString(1, run.getId());
        statement.setString(2, run.getProject().getId());
        statement.setString(3, hostId);
        statement.execute();
      }
    });
  }

  public void remove() {
    handleDatabase(new Job(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("delete from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        statement.execute();
      }
    });
  }

  public void setJobId(String jobId) {
    if (
      handleDatabase(new Job(), new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_JOB_ID + "=?" + " where id=?;");
          statement.setString(1, jobId);
          statement.setString(2, getId());
          statement.execute();
        }
      })
    ) {
      this.jobId = jobId;
    }
  }

  public Path getLocation() {
    Path path = Paths.get( TABLE_NAME + File.separator + name + '_' + shortId );
    return path;
  }

  public Project getProject() {
    if (project == null) {
      project = Project.getInstance(getFromDB(KEY_PROJECT));
    }
    return project;
  }

  public Host getHost() {
    if (host == null) {
      host = Host.getInstance(getFromDB(KEY_HOST));
    }
    return host;
  }

  public Run getRun() {
    if (run == null) {
      run = Run.getInstance(getProject(), getId());
    }
    return run;
  }

  public String getJobId() {
    if (jobId == null) {
      jobId = getFromDB(KEY_JOB_ID);
    }
    return jobId;
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
                KEY_JOB_ID + " default 0," +
                "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }
}
