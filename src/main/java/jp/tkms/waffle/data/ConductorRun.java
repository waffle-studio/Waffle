package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
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
    if (conductorRun[0] != null && State.Created.equals(conductorRun[0].getState())) {
      if (!conductorRun[0].isRunning()) {
        conductorRun[0].setState(State.Failed);
      }
    }

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
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME)
          .where(Sql.Value.equal(KEY_PARENT, parent.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          ConductorRun conductorRun = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString(KEY_NAME)
          );
          list.add(conductorRun);
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
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME +
          " where " + KEY_STATE + "!=" + State.Finished.ordinal() + " and " + KEY_STATE + "!=" + State.Failed.ordinal() + ";");
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
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal( KEY_ID, conductorRun.getId() ),
          Sql.Value.equal( KEY_NAME, conductorRun.getName() ),
          Sql.Value.equal( KEY_PARENT, parent.getId() ),
          Sql.Value.equal( KEY_CONDUCTOR, conductor.getId() ),
          Sql.Value.equal( KEY_PARAMETERS, parent.getParameters().toString() ),
          Sql.Value.equal( KEY_STATE, State.Created.ordinal() )
        ).execute();
      }
    });

    return conductorRun;
  }

  public static ConductorRun create(ConductorRun parent, Conductor conductor) {
    return create(parent.getProject(), parent, conductor);
  }

  public void finish() {
    if (! getState().equals(State.Failed)) {
      setState(State.Finished);
    }
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

  public State getState() {
    return State.valueOf(getIntFromDB(KEY_STATE));
  }

  private void setState(State state) {
    setIntToDB(KEY_STATE, state.ordinal());
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

  public void start() {
    start(false);
  }

  public void start(boolean async) {
    isStarted = true;
    setState(State.Running);
    if (!isRoot()) {
      getParent().setState(State.Running);
    }
    AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    abstractConductor.start(this, async);
  }

  public void update(AbstractRun run) {
    if (!isRoot()) {
      AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
      abstractConductor.eventHandle(this, run);
    }
  }

  @Override
  public void appendErrorNote(String note) {
    super.appendErrorNote(note);
    setState(State.Failed);
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
                + KEY_STATE + ","
                + KEY_ERROR_NOTE + " default '',"
                + KEY_TIMESTAMP_CREATE + " timestamp default (DATETIME('now','localtime'))" +
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
