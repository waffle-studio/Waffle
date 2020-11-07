package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.*;
import org.jruby.Ruby;
import org.jruby.embed.PathType;
import org.json.JSONArray;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class ActorRun extends AbstractRun implements InternalHashedData {
  protected static final String TABLE_NAME = "conductor_run";
  public static final String ROOT_NAME = "ROOT";
  public static final String KEY_ACTOR = "actor";
  public static final String KEY_ACTIVE_RUN = "active_run";
  public static final UUID ROOT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  //private static final Cache<String, Actor> instanceCache = new InstanceCache<Actor>(Actor.class, 500).getCacheStore();
  protected static final HashMap<String, ActorRun> instanceMap = new HashMap<>();

  private String actorName;
  private HashSet<String> activeRunSet = new HashSet<>();
  private boolean isProcessing = false;

  public ActorRun(Project project, UUID id) {
    super(project, id, InternalHashedData.getDataDirectory(project, ActorRun.class.getName(), id));
    instanceMap.put(id.toString(), this);

    synchronized (activeRunSet) {
      for (Object runId : new JSONArray(getStringFromProperty(KEY_ACTIVE_RUN, "[]")).toList()) {
        activeRunSet.add(runId.toString());
      }
    }
  }

  public static ActorRun getInstance(Project project, String id) {
    synchronized (instanceMap) {
      ActorRun actorRun = null;

      //if (!ROOT_UUID.toString().equals(id)) {
        getRootInstance(project);
      //}

      actorRun = instanceMap.get(id);
      if (actorRun == null) {
        actorRun = new ActorRun(project, UUID.fromString(id));
      }

      return actorRun;
    }
  }

  public static ActorRun getRootInstance(Project project) {
    synchronized (instanceMap) {
      ActorRun actorRun = instanceMap.get(ROOT_UUID.toString());
      if (actorRun == null) {
        actorRun = create(ROOT_UUID, RunNode.getRootInstance(project), null, null, null);
      }
      return actorRun;
    }

  }

  public static ActorRun find(Project project, String key) {
    return getInstance(project, key);
  }

  private static ActorRun create(UUID id, RunNode runNode, ActorRun parent, ActorRun actorGroupRun, String actorName) {
    synchronized (instanceMap) {
      Project project = runNode.getProject();
      ActorGroup actorGroup = null;
      if (actorGroupRun != null) {
        actorGroup = actorGroupRun.getActorGroup();
      }
      String actorGroupName = (actorGroup == null ? "" : actorGroup.getName());
      String name = (actorGroup == null ? "NON_CONDUCTOR" : actorGroup.getName())
        + " : " + LocalDateTime.now().toString();

      String callname = getCallName(actorGroup, actorName);
      JSONArray callstack = (parent == null ? new JSONArray() : parent.getCallstack());
      if (callstack.toList().contains(callname)) {
        ActorRun parentActorRun = parent;
        while (parentActorRun != null && !callname.equals(getCallName(parentActorRun.getActorGroup(), parentActorRun.getActorName()))) {
          parentActorRun = parentActorRun.getParentActor();
        }
        if (parentActorRun != null) {
          callstack = parentActorRun.getCallstack();
          runNode = parentActorRun.getRunNode();
        }
      }

      callstack.put(callname);

      ActorRun actorRun = new ActorRun(project, id);

      actorRun.setToProperty(KEY_RUNNODE, runNode.getId());
      actorRun.setToProperty(KEY_OWNER, (actorGroupRun == null ? actorRun.getId() : actorGroupRun.getId()));
      if (parent != null) {
        actorRun.setToProperty(KEY_PARENT, parent.getId());
        actorRun.setToProperty(KEY_RESPONSIBLE_ACTOR, parent.getId());
        //actorRun.setToProperty(KEY_VARIABLES, parent.getVariables().toString());
        actorRun.putVariablesByJson(parent.getVariables().toString());
      }
      actorRun.setToProperty(KEY_ACTOR_GROUP, actorGroupName);
      actorRun.setToProperty(KEY_STATE, State.Created.ordinal());
      actorRun.setToProperty(KEY_ACTOR, actorName);
      actorRun.setToProperty(KEY_CALLSTACK, callstack.toString());

      /*
      if (actorGroup != null) {
        actorRun.putVariablesByJson(actorGroup.getDefaultVariables().toString());
      }
       */

      return getInstance(project, id.toString());
    }
  }

  public static ActorRun create(RunNode runNode, ActorRun parent, ActorRun owner, String actorName) {
    return create(UUID.randomUUID(), runNode, parent, owner, actorName);
  }

  public static ActorRun create(RunNode runNode, ActorRun owner) {
    return create(runNode, owner, owner, ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME);
  }

  private static ActorRun createActorGroupRun(UUID id, RunNode runNode, ActorRun parent, ActorGroup actorGroup) {
    synchronized (instanceMap) {
      Project project = runNode.getProject();
      String actorGroupName = (actorGroup == null ? "" : actorGroup.getName());
      String name = (actorGroup == null ? "NON_CONDUCTOR" : actorGroup.getName())
        + " : " + LocalDateTime.now().toString();

      ActorRun actorRun = new ActorRun(project, id);

      actorRun.setToProperty(KEY_RUNNODE, runNode.getId());
      actorRun.setToProperty(KEY_OWNER, actorRun.getId());
      if (parent != null) {
        actorRun.setToProperty(KEY_PARENT, parent.getId());
        actorRun.setToProperty(KEY_RESPONSIBLE_ACTOR, parent.getId());
        //actorRun.setToProperty(KEY_VARIABLES, parent.getVariables().toString());
        actorRun.putVariablesByJson(parent.getVariables().toString());
      }
      actorRun.setToProperty(KEY_ACTOR_GROUP, actorGroupName);
      actorRun.setToProperty(KEY_STATE, State.Created.ordinal());

      if (actorGroup != null) {
        actorRun.putVariablesByJson(actorGroup.getDefaultVariables().toString());
      }

      return getInstance(project, id.toString());
    }
  }

  public static ActorRun createActorGroupRun(RunNode runNode, ActorRun parent, ActorGroup actorGroup) {
    return createActorGroupRun(UUID.randomUUID(), runNode, parent, actorGroup);
  }

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

  public boolean isActorGroupRun() {
    return getOwner().getId().equals(getId());
  }

  @Override
  public boolean isRunning() {
    synchronized (activeRunSet) {
      return !activeRunSet.isEmpty();
    }
  }

  @Override
  public void start() {
    start(this);
  }

  public void start(AbstractRun caller) {
    start(caller, false);
  }

  public void start(AbstractRun caller, boolean async) {

    super.start();

    isStarted = true;
    setState(State.Running);
    if (!isRoot()) {
      getResponsibleActor().setState(State.Running);
      getResponsibleActor().registerActiveRun(this);
    }

    ActorRun thisInstance = this;
    Thread thread = new Thread() {
      @Override
      public void run() {
        super.run();
        isProcessing = true;
        //processMessage(null); //?????
        if (isActorGroupRun()) {
          //ActorRun actorRun = create(getRunNode().createInclusiveRunNode(getActorGroup().getName()), thisInstance);
          ActorRun actorRun = create(getRunNode(), thisInstance);
          //actorRun.putVariablesByJson(getVariables().toString());
          actorRun.start();
        } else {
          if (!isRoot() && getActorScriptPath() != null && Files.exists(getActorScriptPath())) {
            RubyScript.process((container) -> {
              try {
                container.runScriptlet(RubyConductor.getConductorTemplateScript());
                container.runScriptlet(PathType.ABSOLUTE, getActorScriptPath().toAbsolutePath().toString());
                thisInstance.setFinalizerReference(thisInstance);
                container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_actor_script", thisInstance, caller);
              } catch (Exception e) {
                WarnLogMessage.issue(e);
                getRunNode().appendErrorNote(e.getMessage());
              }
            });
          }
        }
        isProcessing = false;

        if (! isRunning()) {
          setState(jp.tkms.waffle.data.util.State.Finished);
          finish();
        }
        return;
      }
    };
    Main.systemThreadPool.submit(thread);
    if (!async) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }



  @Override
  public void finish() {
    super.finish();
    instanceMap.remove(getId());
  }

  private Path getActorScriptPath() {
    if (ActorGroup.KEY_REPRESENTATIVE_ACTOR_NAME.equals(getActorName())) {
      return getActorGroup().getRepresentativeActorScriptPath();
    }
    return getActorGroup().getActorScriptPath(getActorName());
  }

  public long registerActiveRun(AbstractRun run) {
    synchronized (activeRunSet) {
      activeRunSet.add(run.getId());
      JSONArray jsonArray = new JSONArray();
      for (String id : activeRunSet) {
        jsonArray.put(id);
      }
      setToProperty(KEY_ACTIVE_RUN, jsonArray.toString());
      setState(isRunning() ? State.Running : State.Finished);
      return activeRunSet.size();
    }
  }

  public long removeActiveRun(AbstractRun run) {
    synchronized (activeRunSet) {
      activeRunSet.remove(run.getId());
      JSONArray jsonArray = new JSONArray();
      for (String id : activeRunSet) {
        jsonArray.put(id);
      }
      setToProperty(KEY_ACTIVE_RUN, jsonArray.toString());
      setState(isRunning() ? State.Running : State.Finished);
      return activeRunSet.size();
    }
  }

  public void postMessage(AbstractRun caller, String eventName) {
    long activeRunSetSize = 0;
    if (!caller.isRunning()) {
      activeRunSetSize = removeActiveRun(caller);
    }
    synchronized (this) {
      //processMessage(caller);
      if (activeRunSetSize <= 0 && !isProcessing) {
        setState(State.Finished);
        finish();
      }
    }
  }

  private ConductorTemplate conductorTemplate = null;
  private ListenerTemplate listenerTemplate = null;
  private ArrayList<AbstractRun> transactionRunList = new ArrayList<>();

  public ActorRun createActorGroupRun(String name) {
    ActorGroup actorGroup = ActorGroup.find(getProject(), name);
    if (actorGroup == null) {
      throw new RuntimeException("ActorGroup\"(" + name + "\") is not found");
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

    ActorRun actorRun = ActorRun.createActorGroupRun(getRunNode().createInclusiveRunNode(actorGroup.getName()), this, actorGroup);
    transactionRunList.add(actorRun);

    actorRun.setFinalizerReference(this);

    return actorRun;
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

    transactionRunList.clear();
  }

  @Override
  public String getInternalDataGroup() {
    return ActorRun.class.getName();
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(KEY_ACTOR + "_" + getId() + Constants.EXT_JSON);
    //return getDataDirectory(getUuid()).resolve(KEY_ACTOR + Constants.EXT_JSON);
  }
}
