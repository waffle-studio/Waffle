package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.*;

import java.util.HashMap;
import java.util.HashSet;

public class Hub {

  Project project;
  ConductorRun conductorRun;
  Registry registry;

  String parameterStoreName;

  public Hub(ConductorRun conductorRun) {
    this.project = conductorRun.getProject();
    this.conductorRun = conductorRun;
    this.registry = new Registry(conductorRun.getProject());
    switchParameterStore(null);
  }

  public Project getProject() {
    return project;
  }

  public ConductorRun getConductorRun() {
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

  public ConductorRun createConductorRun(String name) {
    Conductor conductor = Conductor.find(project, name);
    return ConductorRun.create(this.conductorRun, conductor);
  }

  public SimulatorRun createSimulatorRun(String name, String hostName) {
    Simulator simulator = Simulator.find(project, name);
    Host host = Host.find(hostName);
    return SimulatorRun.create(conductorRun, simulator, host);
  }

  public Object getParameter(String key) {
    return registry.get(key);
  }

  public void putParameter(String key, Object value) {
    registry.put(key, value);
  }

  private final ParameterMapInterface parameterMapInterface  = new ParameterMapInterface();
  public HashMap parameters() { return parameterMapInterface; }
  public HashMap p() { return parameterMapInterface; }
  public class ParameterMapInterface extends HashMap<Object, Object> {
    @Override
    public Object get(Object key) {
      return getParameter(key.toString());
    }

    @Override
    public Object put(Object key, Object value) {
      putParameter(key.toString(), value);
      return value;
    }
  }
}
