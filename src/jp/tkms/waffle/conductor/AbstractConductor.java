package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.ConductorRun;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;

abstract public class AbstractConductor {
  abstract protected void mainProcess(ConductorRun run);
  abstract protected void eventHandler(ConductorRun run);
  abstract protected void postProcess(ConductorRun run);
  abstract public String defaultScriptName();
  abstract public void prepareConductor(Conductor conductor);

  public AbstractConductor() {
  }

  public void start(ConductorRun run) {
    mainProcess(run);
  }

  public void eventHandle(ConductorRun run) {
    eventHandler(run);
    if (! run.getTrial().isRunning()) {
      run.remove();
    }
  }

  public static HashMap<String, AbstractConductor> instanceMap = new HashMap<>();

  public static AbstractConductor getInstance(ConductorRun run) {
    return getInstance(run.getConductor().getConductorType());
  }

  public static AbstractConductor getInstance(String className) {
    AbstractConductor conductor = null;

    if (! instanceMap.containsKey(className)) {
      Class<AbstractConductor> clazz = null;
      try {
        clazz = (Class<AbstractConductor>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      Constructor<AbstractConductor> constructor;
      try {
        constructor = clazz.getConstructor();
      } catch (SecurityException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      try {
        conductor = constructor.newInstance();
      } catch (IllegalArgumentException | ReflectiveOperationException e) {
        e.printStackTrace();
      }

      instanceMap.put(className, conductor);
    } else {
      conductor = instanceMap.get(className);
    }

    return conductor;
  }
}
