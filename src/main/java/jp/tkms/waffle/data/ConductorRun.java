package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
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
  private static final String KEY_ARGUMENTS = "arguments";
  private static final String KEY_MODULES = "modules";

  private Trial trial = null;
  private Conductor conductor = null;
  private JSONObject arguments = null;

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

  public JSONObject getArguments() {
    if (arguments == null) {
      JSONObject map = getConductor().getArguments();
      JSONObject valueMap = new JSONObject(getFromDB(KEY_ARGUMENTS));
      for (String key : valueMap.keySet()) {
        map.put(key, valueMap.get(key));
      }
      arguments = map;
    }
    return new JSONObject(arguments.toString());
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

      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_ARGUMENTS + "=? where " + KEY_ID + "=?;");
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

  public ConductorModule getModule(String instanceName) {
    return ConductorModule.getInstance(new JSONObject(getFromDB(KEY_MODULES)).getJSONObject("set").getString(instanceName));
  }

  public List<String> getModuleKeyList() {
    return (List)new JSONObject(getFromDB(KEY_MODULES)).getJSONArray("key").toList();
  }

  public void registerModule(ConductorModule module, String instanceName) {
    JSONObject obj = new JSONObject(getFromDB(KEY_MODULES));
    obj.getJSONArray("key").put(instanceName);
    obj.getJSONObject("set").put(instanceName, module.getId());
    setStringToDB(KEY_MODULES, obj.toString());
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
                + KEY_ARGUMENTS + " default '{}',"
                + KEY_MODULES + " default '{\"key\":[], \"set\":{}}',"
                + "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          }
        ));
      }
    };
  }

  private final ArgumentMapInterface argumentMapInterface  = new ArgumentMapInterface();
  public HashMap arguments() { return argumentMapInterface; }
  public class ArgumentMapInterface extends HashMap<Object, Object> {
    @Override
    public Object get(Object key) {
      return getArgument(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      //putArgument(key.toString(), value);
      return value;
    }
  }
}
