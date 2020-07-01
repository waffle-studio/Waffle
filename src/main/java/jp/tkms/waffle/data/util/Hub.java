package jp.tkms.waffle.data.util;

import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.*;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import java.util.ArrayList;
import java.util.HashMap;

public class Hub {

  Project project;
  Actor conductorRun;
  Actor nextParentConductorRun;
  AbstractRun run;
  Registry registry;
  ArrayList<AbstractRun> createdRunList;

  String parameterStoreName;

  RunNode runNode = null;

  //TODO: do refactor
  ConductorTemplate parentConductorTemplate = null;
  ConductorTemplate conductorTemplate = null;
  ListenerTemplate listenerTemplate = null;

  public Hub(Actor conductorRun, AbstractRun run, ConductorTemplate conductorTemplate) {
    this.project = conductorRun.getProject();
    this.conductorRun = conductorRun;
    this.nextParentConductorRun = conductorRun;
    this.run = run;
    this.registry = new Registry(conductorRun.getProject());
    this.createdRunList = new ArrayList<>();
    switchParameterStore(null);
    parentConductorTemplate = conductorTemplate;

    if (conductorRun.getId().equals(run.getId()) || run instanceof SimulatorRun) {
      runNode = run.getRunNode();
    } else {
      runNode = run.getRunNode().getParent();
    }
  }

  public Hub(Actor conductorRun, AbstractRun run) {
    this(conductorRun, run, null);
  }

  public Project getProject() {
    return project;
  }

  public Actor getConductorRun() {
    return conductorRun;
  }

  public Registry getRegistry() {
    return registry;
  }

  public Registry registry() {
    return getRegistry();
  }

  public void switchParameterStore(String key) {
    if (key == null) {
      parameterStoreName = ".S:" + conductorRun.getId();
    } else {
      parameterStoreName = ".S:" + conductorRun.getId() + '_' + key;
    }
  }

  public void changeParent(String name) {
    nextParentConductorRun = Actor.find(project, name);
  }

  public Actor createActor(String name) {
    ActorGroup actorGroup = ActorGroup.find(project, name);
    if (actorGroup == null) {
      throw new RuntimeException("Conductor\"(" + name + "\") is not found");
    }

    if (runNode instanceof SimulatorRunNode) {
      runNode = ((SimulatorRunNode) runNode).moveToVirtualNode();
    }

    Actor actor = Actor.create(runNode.createInclusiveRunNode(""), nextParentConductorRun, actorGroup);
    createdRunList.add(actor);
    return actor;
  }

  public SimulatorRun createSimulatorRun(String name, String hostName) {
    Simulator simulator = Simulator.find(project, name);
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

    if (runNode instanceof SimulatorRunNode) {
      runNode = ((SimulatorRunNode) runNode).moveToVirtualNode();
    }

    SimulatorRun createdRun = SimulatorRun.create(runNode.createSimulatorRunNode(""), nextParentConductorRun, simulator, host);
    createdRunList.add(createdRun);
    return createdRun;
  }

  public void invokeListener(String name) {
    if (parentConductorTemplate != null) {
      String script = conductorTemplate.getListenerScript(name);
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_template_script", conductorRun, run);
      } catch (EvalFailedException e) {
        BrowserMessage.addMessage("toastr.error('invokeListenerTemplate: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
    } else {
      String script = conductorRun.getConductor().getActorScript(name);
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_script", conductorRun, run);
      } catch (EvalFailedException e) {
        BrowserMessage.addMessage("toastr.error('invokeListener: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
    }
  }

  public void loadConductorTemplate(String name) {
    conductorTemplate = ConductorTemplate.getInstance(name);
  }

  public void loadListenerTemplate(String name) {
    listenerTemplate = ListenerTemplate.getInstance(name);
  }

  public void close() {
    //TODO: do refactor
    if (conductorTemplate != null) {
      String script = conductorTemplate.getMainScript();
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_conductor_template_script", conductorRun, conductorTemplate);
      } catch (EvalFailedException e) {
        BrowserMessage.addMessage("toastr.error('invokeConductorTemplate: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
    } else if (listenerTemplate != null) {
      String script = listenerTemplate.getScript();
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(RubyConductor.getInitScript());
        container.runScriptlet(RubyConductor.getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_template_script", conductorRun, run);
      } catch (EvalFailedException e) {
        BrowserMessage.addMessage("toastr.error('invokeListenerTemplate: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
    }

    if (createdRunList.size() > 1) {
      runNode.switchToParallel();
    }

    for (AbstractRun createdRun : createdRunList) {
      if (! createdRun.isStarted()) {
        createdRun.start();
      }
    }
  }

  public Object getRegistry(String key) {
    return registry.get(key);
  }

  public void putRegistry(String key, Object value) {
    registry.put(key, value);
  }

  private final HashMap<Object, Object> registryMapWrapper  = new HashMap<Object, Object>() {
    @Override
    public Object get(Object key) {
      return getRegistry(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      putRegistry(key.toString(), value);
      return value;
    }
  };
  //public HashMap registry() { return registryMapWrapper; }
  public HashMap r() { return registryMapWrapper; }

  private final HashMap<Object, Object> variablesMapWrapper  = new HashMap<Object, Object>() {
    @Override
    public Object get(Object key) {
      return conductorRun.getVariable(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      conductorRun.putVariable(key.toString(), value);
      return value;
    }
  };
  public HashMap variables() { return variablesMapWrapper; }
  public HashMap v() { return variablesMapWrapper; }

  @Override
  public String toString() {
    return super.toString();
  }

  public void moveToParentNode() {
    if (! runNode.isRoot()) {
      runNode = runNode.getParent();
    }
  }
}
