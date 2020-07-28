package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.data.util.RubyScript;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;
import org.jruby.Ruby;
import org.jruby.embed.PathType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class Actor extends AbstractRun implements InternalHashedData {
  protected static final String TABLE_NAME = "conductor_run";
  public static final String ROOT_NAME = "ROOT";
  public static final String KEY_ACTOR = "actor";
  public static final String KEY_ACTIVE_RUN = "active_run";
  public static final UUID ROOT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private static final HashMap<String, Actor> instanceMap = new HashMap<>();

  private final Object messageProcessorLocker = new Object();
  private String actorName;
  private HashSet<String> activeRunSet = new HashSet<>();

  public Actor(Project project, UUID id) {
    super(project, id, InternalHashedData.getDataDirectory(project, Actor.class.getName(), id));
    instanceMap.put(id.toString(), this);

    for (Object runId : new JSONArray(getStringFromProperty(KEY_ACTIVE_RUN, "[]")).toList()) {
      activeRunSet.add(runId.toString());
    }
  }

  /*
  public Actor(Actor actor) {
    super(actor.getProject(), actor.getUuid(), actor.getName(), actor.getRunNode());
    this.actorName = actor.actorName;
  }
   */

  public static Actor getInstance(Project project, String id) {
    Actor actor = null;

    getRootInstance(project);

    actor = instanceMap.get(id);
    if (actor == null)  {
      actor = new Actor(project, UUID.fromString(id));
    }

    return actor;
  }

  /*
  public static Actor getInstanceByName(Project project, String name) {
    final Actor[] conductorRun = {null};

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR).where(Sql.Value.equal(KEY_NAME, name)).executeQuery();
        while (resultSet.next()) {
          RunNode runNode = RunNode.getInstance(project, resultSet.getString(KEY_RUNNODE));
          if (runNode == null) {
            new Sql.Delete(db, TABLE_NAME).where(Sql.Value.equal(KEY_ID, resultSet.getString(KEY_ID))).execute();
          } else {
            conductorRun[0] = new Actor(
              project,
              UUID.fromString(resultSet.getString(KEY_ID)),
              resultSet.getString(KEY_NAME),
              runNode,
              resultSet.getString(KEY_ACTOR)
            );
          }
        }
      }
    });

    return conductorRun[0];
  }
   */

  public static Actor getRootInstance(Project project) {
    Actor actor = instanceMap.get(ROOT_UUID);
    if (actor == null) {
      actor = create(ROOT_UUID, RunNode.getRootInstance(project), null, null, null);
    }
    return actor;

    /*
    final Actor[] conductorRun = {null};

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR).where(Sql.Value.and(Sql.Value.equal(KEY_NAME, ROOT_NAME), Sql.Value.equal(KEY_PARENT, ""))).executeQuery();
        while (resultSet.next()) {
          conductorRun[0] = new Actor(
            project,
            UUID.fromString(resultSet.getString(KEY_ID)),
            resultSet.getString(KEY_NAME),
            RunNode.getRootInstance(project),
            resultSet.getString(KEY_ACTOR)
          );
        }
      }
    });

    return conductorRun[0];
     */
  }

  public static Actor find(Project project, String key) {
    //if (key.matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")) {
      return getInstance(project, key);
    //}
    //return getInstanceByName(project, key);
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

  public static ArrayList<Actor> getChildList(Project project, Actor parent) {
    ArrayList<Actor> list = new ArrayList<>();

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_RESPONSIBLE_ACTOR, parent.getId()))
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

  public static ArrayList<Actor> getList(Project project, ActorGroup actorGroup) {
    ArrayList<Actor> list = new ArrayList<>();

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_CONDUCTOR, actorGroup.getId()))
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

  public static Actor getLastInstance(Project project, ActorGroup actorGroup) {
    final Actor[] conductorRun = {null};

    handleDatabase(new Actor(project), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_RUNNODE, KEY_ACTOR)
          .where(Sql.Value.equal(KEY_CONDUCTOR, actorGroup.getId())).orderBy(KEY_TIMESTAMP_CREATE, true).limit(1).executeQuery();
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

    return conductorRun[0];
  }

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

    System.out.println(list.size());

    return list;
  }

   */
  private static Actor create(UUID id, RunNode runNode, Actor parent, ActorGroup actorGroup, String actorName) {
    Project project = runNode.getProject();
    String actorGroupName = (actorGroup == null ? "" : actorGroup.getName());
    String name = (actorGroup == null ? "NON_CONDUCTOR" : actorGroup.getName())
      + " : " + LocalDateTime.now().toString();

    String callname = getCallName(actorGroup, actorName);
    JSONArray callstack = (parent == null ? new JSONArray() :parent.getCallstack());
    if (callstack.toList().contains(callname)) {
      Actor parentActor = parent;
      while (parentActor != null && ! callname.equals(getCallName(parentActor.getActorGroup(), parentActor.getActorName()))) {
        parentActor = parentActor.getParentActor();
      }
      if (parentActor != null) {
        callstack = parentActor.getCallstack();
        runNode = parentActor.getRunNode();
      }
    }
    callstack.put(callname);

    Actor actor = new Actor(project, id);

    if (parent != null) {
      actor.setToProperty( KEY_PARENT, parent.getId() );
      actor.setToProperty( KEY_RESPONSIBLE_ACTOR, parent.getId() );
      actor.setToProperty( KEY_VARIABLES, parent.getVariables().toString() );
    }
    actor.setToProperty( KEY_ACTOR_GROUP, actorGroupName );
    actor.setToProperty( KEY_STATE, State.Created.ordinal() );
    actor.setToProperty( KEY_RUNNODE, runNode.getId());
    actor.setToProperty( KEY_ACTOR, actorName);
    actor.setToProperty( KEY_CALLSTACK, callstack.toString());

    return actor;
  }

  public static Actor create(RunNode runNode, Actor parent, ActorGroup actorGroup, String actorName) {
    return create(UUID.randomUUID(), runNode, parent, actorGroup, actorName);
  }

  public static Actor create(RunNode runNode, Actor parent, ActorGroup actorGroup) {
    return create(runNode, parent, actorGroup, ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME);
  }

  /*
  public ArrayList<Actor> getChildActorRunList() {
    //return getChildList(getProject(), this);
    return new ArrayList<>();
  }

  public boolean hasRunningChildRun() {
    return !activeRunSet.isEmpty(); //SimulatorRun.getNumberOfRunning(getProject(), this) > 0;
  }
   */

  public State getState() {
    return State.valueOf(getIntFromProperty(KEY_STATE, 0));
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
    /*
    if (hasRunningChildSimulationRun()) {
      return true;
    }

    for (Actor conductorRun : getChildActorRunList()) {
      if (conductorRun.isRunning()) {
        return true;
      }
    }

    return false;
     */
    return !activeRunSet.isEmpty();
  }

  public void start(AbstractRun caller) {
    start(caller, false);
  }

  public void start(AbstractRun caller, boolean async) {

    super.start();

    /*
    StackTraceElement[] ste = new Throwable().getStackTrace();
    System.out.println("vvvvvvvvvvvvvvvv");
    for (int i = 0; i < ste.length; i++) {
      System.out.println(ste[i].getFileName() + " : " + ste[i].getLineNumber()); // ファイル名を取得
    }
    System.out.println("^^^^^^^^^^^^^^^^");

     */

    isStarted = true;
    setState(State.Running);
    if (!isRoot()) {
      getResponsibleActor().setState(State.Running);
      getResponsibleActor().registerActiveRun(this);
    }
    //AbstractConductor abstractConductor = AbstractConductor.getInstance(this);
    //abstractConductor.start(this, async);

    Actor thisInstance = this;
    Thread thread = new Thread() {
      @Override
      public void run() {
        super.run();
        //processMessage(null); //?????
        if (!isRoot() && getActorScriptPath() != null && Files.exists(getActorScriptPath())) {
          RubyScript.process((container) -> {
            try {
              container.runScriptlet(RubyConductor.getConductorTemplateScript());
              container.runScriptlet(PathType.ABSOLUTE, getActorScriptPath().toAbsolutePath().toString());
              container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_actor_script", thisInstance, caller);
            } catch (Exception e) {
              WarnLogMessage.issue(e);
              getRunNode().appendErrorNote(e.getMessage());
            }
          });
        }

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

  /*
  public void update() {
    if (!isRoot()) {
      //eventHandler(conductorRun, run);
      if (! isRunning()) {
        //if (! run.getState().equals(State.Finished)) { // TOD: check!!!
        //  setState(State.Failed);
        //}

        if (! getState().equals(State.Finished)) {
          setState(State.Finished);
          finish();
        }
      }

      //TODO: do refactor
      //if (getActorGroup() != null) {
      //  int runningCount = 0;
      //  for (Actor notFinished : Actor.getNotFinishedList(getProject()) ) {
      //    if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getName().equals(getActorGroup().getName())) {
      //      runningCount += 1;
      //    }
      //  }
      //  BrowserMessage.addMessage("updateConductorJobNum('" + getActorGroup().getName() + "'," + runningCount + ")");
      //}

    }
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
              new Sql.Create(db, TABLE_NAME, KEY_ID, KEY_NAME, KEY_PARENT, KEY_RESPONSIBLE_ACTOR, KEY_CONDUCTOR,
                Sql.Create.withDefault(KEY_VARIABLES, "'{}'"),
                Sql.Create.withDefault(KEY_FINALIZER, "'[]'"),
                Sql.Create.withDefault(KEY_CALLSTACK, "'[]'"),
                KEY_STATE,
                KEY_RUNNODE,
                KEY_ACTOR,
                Sql.Create.timestamp(KEY_TIMESTAMP_CREATE),
                KEY_PARENT_RUNNODE).execute();
              new Sql.Insert(db, TABLE_NAME,
                Sql.Value.equal(KEY_ID, UUID.randomUUID().toString()),
                Sql.Value.equal(KEY_NAME, ROOT_NAME),
                Sql.Value.equal(KEY_PARENT, ""),
                Sql.Value.equal(KEY_RESPONSIBLE_ACTOR, ""),
                Sql.Value.equal(KEY_RUNNODE, RunNode.getRootInstance(getProject()).getId())// for compatibility
              ).execute();
            }
          }
        ));
      }
    };
  }

   */

  private Path getActorScriptPath() {
    if (ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME.equals(getActorName())) {
      return getActorGroup().getRepresentativeActorScriptPath();
    }
    return getActorGroup().getActorScriptPath(getActorName());
  }

  public void registerActiveRun(AbstractRun run) {
    activeRunSet.add(run.getId());
    JSONArray jsonArray = new JSONArray();
    for (String id : activeRunSet) {
      jsonArray.put(id);
    }
    setToProperty(KEY_ACTIVE_RUN, jsonArray.toString());

    setState(isRunning() ? State.Running : State.Finished);
  }

  public void removeActiveRun(AbstractRun run) {
    activeRunSet.remove(run.getId());
    JSONArray jsonArray = new JSONArray();
    for (String id : activeRunSet) {
      jsonArray.put(id);
    }
    setToProperty(KEY_ACTIVE_RUN, jsonArray.toString());

    setState(isRunning() ? State.Running : State.Finished);
  }

  public void postMessage(AbstractRun caller, String eventName) {
    if (!caller.isRunning()) {
      removeActiveRun(caller);
    }

    //processMessage(caller);
    if (activeRunSet.size() <= 0) {
      setState(State.Finished);
      finish();
    }
  }

  /*
  public void processMessage(AbstractRun caller) {
    if (!isRoot() && getActorScriptPath() != null && Files.exists(getActorScriptPath())) {
      if ("#".equals(getActorName())) {
        try {
          createSimulatorRun("sleep", "LOCAL").start();
        }catch (Throwable e) {
          System.out.println("VVVVVVVVVVVVVVVVVVVV");
          e.printStackTrace();
          System.out.println("^^^^^^^^^^^^^^^^^^^");
        }
      }else {
        RubyScript.process((container) -> {
          try {
            container.runScriptlet(RubyConductor.getConductorTemplateScript());
            container.runScriptlet(PathType.ABSOLUTE, getActorScriptPath().toAbsolutePath().toString());
            container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_actor_script", this, caller);
          } catch (Exception e) {
            WarnLogMessage.issue(e);
            getRunNode().appendErrorNote(e.getMessage());
          }
        });
      }
    }

    if (activeRunSet.size() <= 0) {
      setState(State.Finished);
      finish();
    }
  }

   */

  private ConductorTemplate conductorTemplate = null;
  private ListenerTemplate listenerTemplate = null;
  private ArrayList<AbstractRun> transactionRunList = new ArrayList<>();

  public Actor createActor(String name) {
    ActorGroup actorGroup = ActorGroup.find(getProject(), name);
    if (actorGroup == null) {
      throw new RuntimeException("Conductor\"(" + name + "\") is not found");
    }

    if (getRunNode() instanceof SimulatorRunNode) {
      setRunNode(getRunNode().moveToVirtualNode());
    } else if (getRunNode() instanceof ParallelRunNode) {
      setRunNode(getRunNode().getParent());
    }

    if (transactionRunList.size() == 1) {
      AbstractRun run = transactionRunList.get(0);
      RunNode runNode = run.getRunNode().moveToVirtualNode();
      run.setRunNode(runNode);
      setRunNode(runNode);
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

    if (getRunNode() instanceof SimulatorRunNode) {
      setRunNode(getRunNode().moveToVirtualNode());
    } else if (getRunNode() instanceof ParallelRunNode) {
      setRunNode(getRunNode().getParent());
    }

    if (transactionRunList.size() == 1) {
      AbstractRun run = transactionRunList.get(0);
      RunNode runNode = run.getRunNode().moveToVirtualNode();
      setRunNode(runNode);
    }

    SimulatorRun createdRun = SimulatorRun.create(getRunNode().createSimulatorRunNode(""), this, simulator, host);
    transactionRunList.add(createdRun);

    return createdRun;
  }

  protected void commit() {
    //TODO: do refactor
    if (conductorTemplate != null) {
      RubyScript.process((container) -> {
        String script = listenerTemplate.getScript();
        try {
          container.runScriptlet(RubyConductor.getListenerTemplateScript());
          container.runScriptlet(script);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_conductor_template_script", this, conductorTemplate);
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          getRunNode().appendErrorNote(e.getMessage());
        }
      });
    } else if (listenerTemplate != null) {
      RubyScript.process((container) -> {
        String script = listenerTemplate.getScript();
        try {
          container.runScriptlet(RubyConductor.getListenerTemplateScript());
          container.runScriptlet(script);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_template_script", this, this);
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          getRunNode().appendErrorNote(e.getMessage());
        }
      });
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

  /*
  @Override
  public Path getDirectoryPath() {
    return InternalHashedData.getHashedDirectoryPath(getProject(), getInternalDataGroup(), getUuid());
  }
   */

  @Override
  public String getInternalDataGroup() {
    return Actor.class.getName();
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_ACTOR + Constants.EXT_JSON);
  }
}
