package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.ConductorRun;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;

abstract public class AbstractConductor {
  abstract public void mainProcess(ConductorRun run);
  abstract public void eventHandler(ConductorRun run);

  public AbstractConductor() {
  }

  public void start(ConductorRun run) {
    mainProcess(run);
  }

  public void eventHandle(ConductorRun run) {

  }

  public static HashMap<String, AbstractConductor> instanceMap = new HashMap<>();

  public static AbstractConductor getInstance(ConductorRun run) {
    AbstractConductor conductor = null;

    if (! instanceMap.containsKey(run.getConductor().getConductorType())) {
      Class<AbstractConductor> clazz = null;
      try {
        clazz = (Class<AbstractConductor>) Class.forName(run.getConductor().getConductorType());
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      Class<?>[] types = {ConductorRun.class};
      Constructor<AbstractConductor> constructor;
      try {
        constructor = clazz.getConstructor(types);
      } catch (SecurityException |
        NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      try {
        conductor = constructor.newInstance();
      } catch (IllegalArgumentException | ReflectiveOperationException e) {
        e.printStackTrace();
      }
    } else {

    }

    return conductor;
  }
}
