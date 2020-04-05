package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class ConductorRun extends AbstractRun {
  protected static final String TABLE_NAME = "conductor_run";
  public static final String ROOT_NAME = "ROOT";
  private static final String KEY_PHASE = "phase";

  public ConductorRun(Project project, UUID id, String name) {
    super(project, id, name);
  }

  protected ConductorRun(Project project) {
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

  public static ConductorRun getRootInstance(Project project) {
    final ConductorRun[] conductorRuns = {null};

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet
          = db.executeQuery("select id,name from " + TABLE_NAME + " where name='" + ROOT_NAME + "';");
        while (resultSet.next()) {
          conductorRuns[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return conductorRuns[0];
  }

  public static ConductorRun find(String project, String id) {
    return getInstance(Project.getInstance(project), id);
  }

  public static ArrayList<ConductorRun> getList(Project project, ConductorRun parent) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_PARENT + "=?;");
        statement.setString(1, parent.getId());
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

  public static ArrayList<ConductorRun> getList(Project project, String parentId) {
    return getList(project, getInstance(project, parentId));
  }

  public static ArrayList<ConductorRun> getNotFinishedList(Project project) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + " where " + KEY_PHASE + "=0;");
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

  public static ConductorRun create(Project project, ConductorRun parent, Conductor conductor) {
    ConductorRun conductorRun = new ConductorRun(project, UUID.randomUUID(), conductor.getName() + " : " + LocalDateTime.now().toString());

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = new Sql.Insert(db, TABLE_NAME,
          KEY_ID,
          KEY_NAME,
          KEY_PARENT,
          KEY_CONDUCTOR
          ).toPreparedStatement();
        statement.setString(1, conductorRun.getId());
        statement.setString(2, conductorRun.getName());
        statement.setString(3, parent.getId());
        statement.setString(4, conductor.getId());
        statement.execute();
      }
    });

    return conductorRun;
  }

  public static ConductorRun create(ConductorRun parent, Conductor conductor) {
    return create(parent.getProject(), parent, conductor);
  }

  public void finish() {
    setIntToDB(KEY_PHASE, 1);
    if (!isRoot()) {
      getParent().update(this);
    }
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  public ArrayList<ConductorRun> getChildConductorRunList() {
    return getList(getProject(), this);
  }

  public ArrayList<SimulatorRun> getChildSimulationRunList() {
    return SimulatorRun.getList(getProject(), this);
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  @Override
  public boolean isRunning() {
    for (SimulatorRun run : getChildSimulationRunList()) {
      if (run.isRunning()) {
        return true;
      }
    }

    for (ConductorRun conductorRun : getChildConductorRunList()) {
      if (conductorRun.isRunning()) {
        return true;
      }
    }

    return false;
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
    if (!isRoot()) {
      AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
      abstractConductor.eventHandle(this, run);
    }
  }

  public void addFinalizer(String name) {
    addFinalizer(this, name);
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
                "id,name," + KEY_PARENT + "," + KEY_CONDUCTOR + ","
                + KEY_PARAMETERS + " default '{}',"
                + KEY_FINALIZER + " default '[]',"
                + KEY_PHASE + " default 0,"
                + "timestamp_create timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          },
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              PreparedStatement statement = db.preparedStatement("insert into " + TABLE_NAME + "(" +
                "id,name," +
                KEY_PARENT +
                ") values(?,?,?);");
              statement.setString(1, UUID.randomUUID().toString());
              statement.setString(2, ROOT_NAME);
              statement.setString(3, "");
              statement.execute();
            }
          }
        ));
      }
    };
  }
}
