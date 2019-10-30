package jp.tkms.waffle.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Run extends ProjectData{
  protected static final String TABLE_NAME = "run";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_HOST = "host";
  private static final String KEY_TRIALS = "trials";

  private String conductor;
  private String host;
  private String trials;

  private Run(Conductor conductor, Host host, Trials trials) {
    this(conductor.getProject(), UUID.randomUUID(), conductor.getId(), host.getId(), trials.getId());
  }

  private Run(Project project, UUID id, String conductor, String host, String trials) {
    super(project, id, "");
    this.conductor = conductor;
    this.host = host;
    this.trials = trials;
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

  public static ArrayList<Run> getList(Project project, Trials parent) {
    ArrayList<Run> list = new ArrayList<>();
    try {
      Database db = getWorkDB(project, workDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id,"
        + KEY_CONDUCTOR + "," + KEY_HOST + "," + KEY_TRIALS
        + " from " + TABLE_NAME + " where " + KEY_TRIALS + "=?;");
      statement.setString(1, parent.getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        list.add(new Run(
          project,
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString(KEY_CONDUCTOR),
          resultSet.getString(KEY_HOST),
          resultSet.getString(KEY_TRIALS)
        ));
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static Run create(Conductor conductor, Host host, Trials trials) {
    Run run = new Run(conductor, host, trials);

    try {
      Database db = getWorkDB(conductor.getProject(), workDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,"
        + KEY_CONDUCTOR + ","
        + KEY_HOST + ","
        + KEY_TRIALS
        + ") values(?,?,?,?);");
      statement.setString(1, run.getId());
      statement.setString(2, run.getConductor().getId());
      statement.setString(3, run.getHost().getId());
      statement.setString(4, run.getTrials().getId());
      statement.execute();
      db.commit();
      db.close();

      Job.addRun(run);

      //Files.createDirectories(simulator.getLocation());
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return run;
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
              KEY_HOST + "," +
              KEY_TRIALS + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
