package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.TestConductor;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ConductorRun extends AbstractRun {
  protected static final String TABLE_NAME = "conductor_run";
  private static final String KEY_TRIAL = "trial";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_ARGUMENTS = "arguments";

  private Trial trial = null;
  private Conductor conductor = null;
  private JSONObject arguments = null;

  public ConductorRun(Project project, UUID id) {
    super(project, id, "");
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ConductorRun getInstance(Project project, String id) {
    final ConductorRun[] conductorRun = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id"))
          );
        }
      }
    });

    return conductorRun[0];
  }

  public static ConductorRun find(String project, String id) {
    return getInstance(Project.getInstance(project), id);
  }

  public static ArrayList<ConductorRun> getList(Trial trial) {
    Project project = trial.getProject();
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_TRIAL + "=?;");
        statement.setString(1, trial.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id"))
          ));
        }
      }
    });

    return list;
  }

  public static ArrayList<ConductorRun> getList(Project project) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          list.add(new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id"))
          ));
        }
      }
    });

    return list;
  }

  public static ConductorRun create(Project project, Trial trial, Conductor conductor) {
    ConductorRun conductorRun = new ConductorRun(project, UUID.randomUUID());

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("insert into " + TABLE_NAME + "(id," +
          KEY_TRIAL + ","
          + KEY_CONDUCTOR + ") values(?,?,?);");
        statement.setString(1, conductorRun.getId());
        statement.setString(2, trial.getId());
        statement.setString(3, conductor.getId());
        statement.execute();
      }
    });

    return conductorRun;
  }

  public static ConductorRun create(ConductorRun parent, Conductor conductor) {
    return create(parent.getProject(), parent.getTrial(), conductor);
  }

  public void remove() {
    if (handleWorkDB(getProject(), workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("delete from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        statement.execute();
      }
    })) {
      Trial parent = getTrial().getParent();
      if (parent != null) {
        for (ConductorRun run : ConductorRun.getList(parent)) {
          run.update();
        }
      }
    }
  }

  public void setTrial(Trial trial) {
    String trialId = trial.getId();
    if (
      handleWorkDB(getProject(), workUpdater, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("update " + getTableName() + " set " + KEY_TRIAL + "=?" + " where id=?;");
          statement.setString(1, trialId);
          statement.setString(2, getId());
          statement.execute();
        }
      })
    ) {
      this.trial = trial;
    }
  }

  public Trial getTrial() {
    if (trial == null) {
      trial = Trial.getInstance(getProject(), getFromDB(KEY_TRIAL));
    }
    return trial;
  }

  public Conductor getConductor() {
    if (conductor == null) {
      conductor = Conductor.getInstance(getProject(), getFromDB(KEY_CONDUCTOR));
    }
    return conductor;
  }

  public JSONObject getArguments() {
    if (arguments == null) {
      JSONObject map = getConductor().getArguments();
      JSONObject valueMap = new JSONObject(getFromDB(KEY_ARGUMENTS));
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
      }
      arguments = map;
    }
    return new JSONObject(arguments);
  }

  public Object getArgument(String key) {
    return getArguments().get(key);
  }

  public void putArguments(String json) {
    getArguments();
    JSONObject valueMap = null;
    try {
      valueMap = new JSONObject(json);
    } catch (Exception e) {
      BrowserMessage.addMessage("toastr.error('json: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
    JSONObject map = new JSONObject(getFromDB(KEY_ARGUMENTS));
    if (valueMap != null) {
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
        arguments.put(key, valueMap.get(key));
      }

      handleWorkDB(getProject(), workUpdater, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_ARGUMENTS + "=? where " + KEY_ID + "=?;");
          new Sql.Select(db, getTableName(), KEY_ARGUMENTS).where(Sql.Value.equalP(KEY_ID)).preparedStatement();
          statement.setString(1, map.toString());
          statement.setString(2, getId());
          statement.execute();
        }
      });
    }
  }

  public void putArgument(String key, Object value) {
    JSONObject obj = new JSONObject();
    obj.put(key, value);
    putArguments(obj.toString());
  }

  public void start() {
    AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    abstractConductor.start(this);
  }

  public void update() {
    AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    abstractConductor.eventHandle(this);
  }

  @Override
  protected Updater getMainUpdater() {
    return null;
  }

  @Override
  protected Updater getWorkUpdater() {
    return workUpdater;
  }

  private static Updater workUpdater = new Updater() {
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
              "id,name," + KEY_TRIAL + "," + KEY_CONDUCTOR + "," + KEY_ARGUMENTS + " default '{}'," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };
}
