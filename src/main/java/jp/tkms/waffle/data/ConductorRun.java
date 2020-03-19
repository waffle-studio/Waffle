package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class ConductorRun extends AbstractRun {
  protected static final String TABLE_NAME = "conductor_run";
  private static final String KEY_CONDUCTOR = "conductor";
  private static final String KEY_TRIAL = "trial";

  private Trial trial = null;
  private Conductor conductor = null;

  public ConductorRun(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public ConductorRun(Project project) {
    super(project);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static ConductorRun getInstance(Project project, String id) {
    final ConductorRun[] conductorRun = {null};

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString(KEY_NAME)
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

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_TRIAL + "=?;");
        statement.setString(1, trial.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString(KEY_NAME)
          ));
        }
      }
    });

    return list;
  }

  public static ArrayList<ConductorRun> getList(Project project) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          list.add(new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString(KEY_NAME)
          ));
        }
      }
    });

    return list;
  }

  public static ConductorRun create(Project project, Trial trial, Conductor conductor) {
    ConductorRun conductorRun = new ConductorRun(project, UUID.randomUUID(), conductor.getName() + " : " + LocalDateTime.now().toString());

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Insert(db, TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_TRIAL,
          KEY_CONDUCTOR
          ).toPreparedStatement();
        statement.setString(1, conductorRun.getId());
        statement.setString(2, conductorRun.getName());
        statement.setString(3, trial.getId());
        statement.setString(4, conductor.getId());
        statement.execute();
      }
    });

    return conductorRun;
  }

  public static ConductorRun create(ConductorRun parent, Conductor conductor) {
    return create(parent.getProject(), parent.getTrial(), conductor);
  }

  public void remove() {
    if (handleDatabase(this, new Handler() {
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
        for (ConductorRun entity : ConductorRun.getList(parent)) {
          entity.update(this);
        }
      }
    }
  }

  public void setTrial(Trial trial) {
    String trialId = trial.getId();
    if (
      handleDatabase(this, new Handler() {
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

  public JSONObject getParameters() {
    return getTrial().getParameters();
  }

  public Object getParameter(String key) {
    return getParameters().get(key);
  }

  public void putParametersByJson(String json) {
    getTrial().putParametersByJson(json);
  }

  public void putParameter(String key, Object value) {
    getTrial().putParameter(key, value);
  }

  public JSONObject getNextRunParameters(Simulator simulator) {
    String registryKey = ".DP:" + getId() + ":" + simulator.getId();
    String simulatorDefaultParametersJson = Registry.getString(getProject(), registryKey, null);
    if (simulatorDefaultParametersJson == null) {
      JSONObject parameters = ParameterGroup.getRootInstance(simulator).toJSONObject();
      Registry.set(getProject(), registryKey, parameters.toString());
      return parameters;
    }
    JSONObject nextParameters = new JSONObject(simulatorDefaultParametersJson);
    updateParameterDefaultValue(nextParameters, ParameterGroup.getRootInstance(simulator));
    Registry.set(getProject(), registryKey, nextParameters.toString());
    return nextParameters;
  }

  private void updateParameterDefaultValue(JSONObject defaultParameters, ParameterGroup targetGroup) {
    for (Parameter parameter : Parameter.getList(targetGroup)) {
      parameter.updateDefaultValue(this, defaultParameters);
    }
    for (ParameterGroup group : ParameterGroup.getList(targetGroup)) {
      updateParameterDefaultValue(defaultParameters.getJSONObject(group.getName()), group);
    }
  }

  public void start() {
    AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    abstractConductor.start(this);
  }

  public void update(AbstractRun run) {
    AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    abstractConductor.eventHandle(this, run);
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
                "id,name," + KEY_TRIAL + "," + KEY_CONDUCTOR + ","
                + KEY_FINALIZER + " default '[]',"
                + "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

  private final ParameterMapInterface parameterMapInterface = new ParameterMapInterface();
  public HashMap arguments() { return parameterMapInterface; }
  public class ParameterMapInterface extends HashMap<Object, Object> {
    @Override
    public Object get(Object key) {
      return getParameter(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      //putArgument(key.toString(), value);
      return value;
    }
  }
}
