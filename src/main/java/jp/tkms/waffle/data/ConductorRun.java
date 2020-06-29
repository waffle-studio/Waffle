package jp.tkms.waffle.data;

import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.EmptyConductor;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;

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
  public static final String ROOT_NAME = "trial";

  public ConductorRun(Project project, UUID id, String name, RunNode runNode) {
    super(project, id, name, runNode);
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
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
        }
      }
    });

    return conductorRun[0];
  }

  public static ConductorRun getInstanceByName(Project project, String name) {
    final ConductorRun[] conductorRun = {null};

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE).where(Sql.Value.equal(KEY_NAME, name)).executeQuery();
        while (resultSet.next()) {
          RunNode runNode = RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE));
          if (runNode == null) {
            new Sql.Delete(db, TABLE_NAME).where(Sql.Value.equal(KEY_ID, resultSet.getString(KEY_ID))).execute();
          } else {
            conductorRun[0] = new ConductorRun(
              project,
              UUID.fromString(resultSet.getString(KEY_ID)),
              resultSet.getString(KEY_NAME),
              runNode
            );
          }
        }
      }
    });

    return conductorRun[0];
  }

  public static ConductorRun getRootInstance(Project project) {
    final ConductorRun[] conductorRun = {null};

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE).where(Sql.Value.and(Sql.Value.equal(KEY_NAME, ROOT_NAME), Sql.Value.equal(KEY_PARENT, ""))).executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getRootInstance(project)
          );
        }
      }
    });

    return conductorRun[0];
  }

  public static ConductorRun find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  public static ArrayList<ConductorRun> getList(Project project, ConductorRun parent) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE)
          .where(Sql.Value.equal(KEY_PARENT, parent.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          ConductorRun conductorRun = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
          list.add(conductorRun);
        }
      }
    });

    return list;
  }

  public static ArrayList<ConductorRun> getList(Project project, Conductor conductor) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE)
          .where(Sql.Value.equal(KEY_CONDUCTOR, conductor.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          ConductorRun conductorRun = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
          list.add(conductorRun);
        }
      }
    });

    return list;
  }

  public static ConductorRun getLastInstance(Project project, Conductor conductor) {
    final ConductorRun[] conductorRun = {null};

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name," + KEY_RUNNODE + " from " + TABLE_NAME + " where conductor=? order by " + KEY_TIMESTAMP_CREATE + " desc, " + KEY_ROWID + " desc limit 1;");
        statement.setString(1, conductor.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new ConductorRun(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          );
        }
      }
    });

    return conductorRun[0];
  }

  public static ArrayList<ConductorRun> getList(Project project, String parentId) {
    return getList(project, getInstance(project, parentId));
  }

  public static ArrayList<ConductorRun> getNotFinishedList(Project project) {
    ArrayList<ConductorRun> list = new ArrayList<>();

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name," + KEY_RUNNODE + " from " + TABLE_NAME +
          " where "
          + KEY_STATE + "!=" + State.Finished.ordinal() + " and "
          + KEY_STATE + "!=" + State.Excepted.ordinal() + " and "
          + KEY_STATE + "!=" + State.Canceled.ordinal() + " and "
          + KEY_STATE + "!=" + State.Failed.ordinal() + ";");
        while (resultSet.next()) {
          list.add(new ConductorRun(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE))
          ));
        }
      }
    });

    return list;
  }

  public static ConductorRun create(Project project, ConductorRun parent, Conductor conductor, RunNode runNode) {
    String conductorId = (conductor == null ? "" : conductor.getId());
    String conductorName = (conductor == null ? "NON_CONDUCTOR" : conductor.getName());
    String name = conductorName + " : " + LocalDateTime.now().toString();
    ConductorRun conductorRun = new ConductorRun(project, UUID.randomUUID(), name, runNode);

    handleDatabase(new ConductorRun(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal( KEY_ID, conductorRun.getId() ),
          Sql.Value.equal( KEY_NAME, conductorRun.getName() ),
          Sql.Value.equal( KEY_PARENT, parent.getId() ),
          Sql.Value.equal( KEY_CONDUCTOR, conductorId ),
          Sql.Value.equal( KEY_VARIABLES, parent.getVariables().toString() ),
          Sql.Value.equal( KEY_STATE, State.Created.ordinal() ),
          Sql.Value.equal( KEY_RUNNODE, runNode.getId())
        ).execute();
      }
    });

    return conductorRun;
  }

  public static ConductorRun create(ConductorRun parent, Conductor conductor, RunNode runNode) {
    return create(parent.getProject(), parent, conductor, runNode);
  }

  public void finish() {
    if (!(getState().equals(State.Failed) || getState().equals(State.Excepted) || getState().equals(State.Canceled))) {
      setState(State.Finished);
    }
    if (!isRoot()) {
      getParent().update(this);
    }
  }

  public ArrayList<ConductorRun> getChildConductorRunList() {
    return getList(getProject(), this);
  }

  public boolean hasRunningChildSimulationRun() {
    return SimulatorRun.getNumberOfRunning(getProject(), this) > 0;
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getDirectoryPath().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  public State getState() {
    return State.valueOf(getIntFromDB(KEY_STATE));
  }

  public void setState(State state) {
    setToDB(KEY_STATE, state.ordinal());
  }

  @Override
  public boolean isRunning() {
    if (hasRunningChildSimulationRun()) {
      return true;
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
      if (this.getConductor() != null) {
        AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
        abstractConductor.eventHandle(this, run);
      } else {
        new EmptyConductor().eventHandle(this, run);
      }

      //TODO: do refactor
      if (getConductor() != null) {
        int runningCount = 0;
        for (ConductorRun notFinished : ConductorRun.getNotFinishedList(getProject()) ) {
          if (notFinished.getConductor() != null && notFinished.getConductor().getId().equals(getConductor().getId())) {
            runningCount += 1;
          }
        }
        BrowserMessage.addMessage("updateConductorJobNum('" + getConductor().getId() + "'," + runningCount + ")");
      }
    }
  }

  @Override
  public void appendErrorNote(String note) {
    super.appendErrorNote(note);
    //setState(State.Failed);
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
                + KEY_VARIABLES + " default '{}',"
                + KEY_FINALIZER + " default '[]',"
                + KEY_STATE + ","
                + KEY_RUNNODE + ","
                + KEY_ERROR_NOTE + " default '',"
                + KEY_TIMESTAMP_CREATE + " timestamp default (DATETIME('now','localtime'))" +
                ");");
            }
          },
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              new Sql.Insert(db, TABLE_NAME,
                Sql.Value.equal(KEY_ID, UUID.randomUUID().toString()),
                Sql.Value.equal(KEY_NAME, ROOT_NAME),
                Sql.Value.equal(KEY_PARENT, ""),
                Sql.Value.equal(KEY_RUNNODE, RunNode.getRootInstance(getProject()).getId())
                ).execute();
            }
          }
        ));
      }
    };
  }
}
