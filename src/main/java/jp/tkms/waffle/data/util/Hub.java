package jp.tkms.waffle.data.util;

import jp.tkms.waffle.conductor.module.RubyConductorModule;
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

  public void registerModule(String key, String name) {
    ConductorModule module = ConductorModule.getInstance(key);
    if (module != null) {
      String originalName = name;
      int conflictCount = 0;
      while (registerNameSet.containsKey(name)) {
        conflictCount += 1;
        name = originalName  + '_' + conflictCount;
      }
      registerNameSet.put(name, key);

      RubyConductorModule.getInstance().registerDefaultParameters(conductorRun, module, name);
    }
  }

  public void register_module(String key, String name) {
    registerModule(key, name);
  }

  public void register_module(String key) {
    registerModule(key, key);
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
