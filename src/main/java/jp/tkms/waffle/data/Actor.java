package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.json.JSONArray;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class Actor extends AbstractRun {
  protected static final String TABLE_NAME = "conductor_run";
  public static final String ROOT_NAME = "ROOT";
  public static final String KEY_ACTOR = "actor";
  private static final String ACTOR_PREFIX = "#";

  private static final HashMap<String, Actor> instanceMap = new HashMap<>();
  private static final String KEY_RUNNING_CHILD_ACTOR = "running_actor";

  private String actorName = null;

  protected Actor(Project project, UUID id, String name) {
    super(project, id, name);
  }

  public Actor(Actor actor) {
    super(actor.getProject(), actor.getUuid(), actor.getName());
  }

  protected Actor(Project project) {
    super(project);
  }

  public static Path getLocalPath(String name) {
    return Paths.get(name + Constants.EXT_JSON);
  }

  public Path getLocalPath() {
    return getLocalPath(getName());
  }

  @Override
  protected Path getPropertyStorePath() {
    return getProject().getDirectoryPath().resolve(getName() + Constants.EXT_JSON);
  }

  @Override
  public Path getDirectoryPath() {
    return getPropertyStorePath().getParent();
  }

  public static Actor getInstance(Project project, String id) {
    if (id == null || "".equals(id)) {
      return null;
    }

    DataId dataId = DataId.getInstance(id);
    Actor actor = instanceMap.get(dataId.getId());
    if (actor != null) {
      return actor;
    }

    actor = new Actor(project, dataId.getUuid(), project.getDirectoryPath().relativize(dataId.getPath()).toString());
    instanceMap.put(dataId.getId(), actor);

    return actor;
  }

  public static Actor getInstanceByName(Project project, String name) {
    if (name == null || "".equals(name)) {
      return null;
    }

    DataId dataId = DataId.getInstance(Actor.class, project.getDirectoryPath().resolve(name));
    Actor actor = instanceMap.get(dataId.getId());
    if (actor != null) {
      return actor;
    }

    actor = new Actor(project, dataId.getUuid(), project.getDirectoryPath().relativize(dataId.getPath()).toString());
    instanceMap.put(dataId.getId(), actor);

    return actor;
  }

  public static Actor getRootInstance(Project project) {
    RunNode runNode = RunNode.getRootInstance(project);
    DataId dataId = DataId.getInstance(Actor.class, runNode.getDirectoryPath());
    Actor actor = instanceMap.get(dataId.getId());
    if (actor != null) {
      return actor;
    }

    actor = new Actor(project, dataId.getUuid(), ROOT_NAME);
    actor.setToProperty(KEY_PARENT, "");
    actor.setToProperty(KEY_RESPONSIBLE_ACTOR, "");
    actor.setToProperty(KEY_RUNNODE, runNode.getId());

    instanceMap.put(dataId.getId(), actor);
    return actor;
  }

  public static Actor find(Project project, String key) {
    if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    }
    return getInstanceByName(project, key);
  }

  /*
  public static ArrayList<Actor> getList(Project project, Actor parent) {
    ArrayList<Actor> list = new ArrayList<>();

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_PARENT, parent.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          Actor conductorRun = new Actor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE)),
            resultSet.getString(KEY_ACTOR)
          );
          list.add(conductorRun);
        }
      }
    });

    return list;
  }

   */

  /*
  public static ArrayList<Actor> getList(Project project, ActorGroup actorGroup) {
    ArrayList<Actor> list = new ArrayList<>();

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_ACTOR_GROUP, actorGroup.getId()))
          .orderBy(KEY_TIMESTAMP_CREATE, true).orderBy(KEY_ROWID, true).executeQuery();
        while (resultSet.next()) {
          Actor conductorRun = new Actor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE)),
            resultSet.getString(KEY_ACTOR)
          );
          list.add(conductorRun);
        }
      }
    });

    return list;
  }
   */

  public static Actor getLastInstance(Project project, ActorGroup actorGroup) {
    final Actor[] conductorRun = {null};

    /*
    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_ACTOR_GROUP, actorGroup.getId())).orderBy(KEY_TIMESTAMP_CREATE, true).limit(1).executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new Actor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE)),
            resultSet.getString(KEY_ACTOR)
          );
        }
      }
    });

     */

    return conductorRun[0];
  }

  /*
  public static ArrayList<Actor> getList(Project project, String parentId) {
    return getList(project, getInstance(project, parentId));
  }

  public static ArrayList<Actor> getNotFinishedList(Project project) {
    ArrayList<Actor> list = new ArrayList<>();

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.lessThan(KEY_STATE, State.Finished.ordinal())).executeQuery();
        while (resultSet.next()) {
          list.add(new Actor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE)),
            resultSet.getString(KEY_ACTOR)
          ));
        }
      }
    });

    return list;
  }
   */

  public static Actor create(RunNode runNode, Actor parent, ActorGroup actorGroup, String actorName) {

    Project project = parent.getProject();
    String conductorId = (actorGroup == null ? "" : actorGroup.getId());
    String conductorName = (actorGroup == null ? "NON_CONDUCTOR" : actorGroup.getName());
    String name = ACTOR_PREFIX + conductorName + "_" + LocalDateTime.now().toString() + "_" + UUID.randomUUID().toString();
    DataId dataId = DataId.getInstance(Actor.class, runNode.getDirectoryPath().resolve(getLocalPath(name)));

    Actor actor = new Actor(project, dataId.getUuid(), project.getDirectoryPath().relativize(dataId.getPath()).toString());
    actor.setRunNode(runNode);

    actor.setToProperty(KEY_PARENT, parent.getId());
    actor.setToProperty(KEY_RESPONSIBLE_ACTOR, parent.getId());
    actor.setToProperty(KEY_ACTOR_GROUP, conductorId);
    actor.setToProperty(KEY_VARIABLES, parent.getVariables().toString());
    actor.setToProperty(KEY_STATE, State.Created.ordinal());
    actor.setToProperty(KEY_RUNNODE, runNode.getId());
    actor.setToProperty(KEY_ACTOR, actorName);

    return actor;
  }

  public static Actor create(RunNode runNode, Actor parent, ActorGroup actorGroup) {
    return create(runNode, parent, actorGroup, ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME);
  }

  public JSONArray getRunningChildActorIdRunList() {
    return getArrayFromProperty(KEY_RUNNING_CHILD_ACTOR, false);
  }

  public boolean hasRunningChildSimulationRun() {
    return SimulatorRun.getNumberOfRunning(getProject(), this) > 0;
  }

  public State getState() {
    return State.valueOf(getIntFromProperty(KEY_STATE, State.Created.ordinal()));
  }

  public void setState(State state) {
    setToProperty(KEY_STATE, state.ordinal());
  }

  public String getActorName() {
    if (actorName == null) {
      actorName = getStringFromProperty(KEY_ACTOR);
    }
    return actorName;
  }

  @Override
  public boolean isRunning() {
    if (hasRunningChildSimulationRun()) {
      return true;
    }

    if (getRunningChildActorIdRunList().length() > 0) {
      return true;
    }

    return false;
  }

  public void start() {
    start(false, getParentActor());
  }

  public void start(AbstractRun caller) {
    start(false, caller);
  }

  public void start(boolean async) {
    start(async, getParentActor());
  }

  public void start(boolean async, AbstractRun caller) {
    /*
    StackTraceElement[] ste = new Throwable().getStackTrace();
    System.out.println("vvvvvvvvvvvvvvvv");
    for (int i = 0; i < ste.length; i++) {
      System.out.println(ste[i].getFileName() + " : " + ste[i].getLineNumber()); // ファイル名を取得
    }
    System.out.println("^^^^^^^^^^^^^^^^");

     */

    if (! isStarted) {
      isStarted = true;
      setState(State.Running);
      if (!isRoot()) {
        getResponsibleActor().setState(State.Running);
      }
    }
    //AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    //abstractConductor.start(this, async);

    Thread thread = new Thread() {
      @Override
      public void run() {
        super.run();
        processMessage(caller); //?????

        if (! isRunning()) {
          setState(jp.tkms.waffle.data.util.State.Finished);
          finish();
        }
        return;
      }
    };
    thread.start();
    if (!async) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public void update() {
    if (!isRoot()) {
      //eventHandler(conductorRun, run);
      if (! isRunning()) {
        /*
        if (! run.getState().equals(State.Finished)) { // TOD: check!!!
          setState(State.Failed);
        }
         */

        if (! getState().equals(State.Finished)) {
          setState(State.Finished);
          finish();
        }
      }

      //TODO: do refactor
      /*
      if (getActorGroup() != null) {
        int runningCount = 0;
        for (Actor notFinished : Actor.getNotFinishedList(getProject()) ) {
          if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getId().equals(getActorGroup().getId())) {
            runningCount += 1;
          }
        }
        BrowserMessage.addMessage("updateConductorJobNum('" + getActorGroup().getId() + "'," + runningCount + ")");
      }
       */
    }
  }

  private Path getActorScriptPath() {
    if (ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME.equals(getActorName())) {
      return getActorGroup().getRepresentativeActorScriptPath();
    }
    return getActorGroup().getActorScriptPath(getActorName());
  }

  public void processMessage(AbstractRun caller) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(RubyConductor.getInitScript());
      container.runScriptlet(RubyConductor.getConductorTemplateScript());
    } catch (EvalFailedException e) {
      ErrorLogMessage.issue(e);
    }
    try {
      container.runScriptlet(PathType.ABSOLUTE, getActorScriptPath().toAbsolutePath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_actor_script", this, caller);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      getRunNode().appendErrorNote(e.getMessage());
    }
    container.terminate();
  }

  private ConductorTemplate conductorTemplate = null;
  private ListenerTemplate listenerTemplate = null;
  private ArrayList<AbstractRun> transactionRunList = new ArrayList<>();

  public Actor createActor(String name) {
    ActorGroup actorGroup = ActorGroup.find(getProject(), name);
    if (actorGroup == null) {
      throw new RuntimeException("Conductor\"(" + name + "\") is not found");
    }

    if (getRunNode() instanceof SimulatorRunNode) {
      setRunNode(((SimulatorRunNode) getRunNode()).moveToVirtualNode());
    }

    Actor actor = Actor.create(getRunNode().createInclusiveRunNode(""), this, actorGroup);
    transactionRunList.add(actor);
    return actor;
  }

  public SimulatorRun createSimulatorRun(String name, String hostName) {
    Simulator simulator = Simulator.find(getProject(), name);
    if (simulator == null) {
      throw new RuntimeException("Simulator(\"" + name + "\") is not found");
    }
    Host host = Host.find(hostName);
    if (host == null) {
      throw new RuntimeException("Host(\"" + hostName + "\") is not found");
    }
    //host.update();
    if (! host.getState().equals(HostState.Viable)) {
      throw new RuntimeException("Host(\"" + hostName + "\") is not viable");
    }

    SimulatorRun createdRun = null;
    if (getRunNode() instanceof SimulatorRunNode) {
      //setRunNode(((SimulatorRunNode) getRunNode()).moveToVirtualNode());
      createdRun = SimulatorRun.create(getRunNode().getParent().createSimulatorRunNode(""), this, simulator, host);
    } else {
      createdRun = SimulatorRun.create(getRunNode().createSimulatorRunNode(""), this, simulator, host);
    }

    transactionRunList.add(createdRun);
    return createdRun;
  }

  protected void commit() {
    //TODO: do refactor
    if (conductorTemplate != null) {
      String script = conductorTemplate.getMainScript();
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_conductor_template_script", this, conductorTemplate);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
      container.terminate();
    } else if (listenerTemplate != null) {
      String script = listenerTemplate.getScript();
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_template_script", this, this);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
      container.terminate();
    }

    if (transactionRunList.size() > 1) {
      getRunNode().switchToParallel();
    }

    for (AbstractRun createdRun : transactionRunList) {
      if (! createdRun.isStarted()) {
        createdRun.start();
      }
    }

    transactionRunList.clear();;
  }
}
