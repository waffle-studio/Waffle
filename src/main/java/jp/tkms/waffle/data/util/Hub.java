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

  public ConductorRun getConductorRun() {
    return conductorRun;
  }

  public ConductorRun conductorRun() {
    return getConductorRun();
  }

  public Registry getRegistry() {
    return registry;
  }

  public Registry registry() {
    return getRegistry();
  }

  public ConductorRun createConductorRun(String key) {
    Conductor conductor = Conductor.getInstance(project, key);
    return ConductorRun.create(conductorRun, conductor);
  }

  public void switchParameterStore(String key) {
    if (key == null) {
      parameterStoreName = ".S:" + conductorRun.getId();
    } else {
      parameterStoreName = ".S:" + conductorRun.getId() + '_' + key;
    }
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
