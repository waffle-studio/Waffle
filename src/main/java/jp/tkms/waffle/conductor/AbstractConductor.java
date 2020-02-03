package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.AbstractRun;
import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.ConductorEntity;
import jp.tkms.waffle.data.Run;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.HashMap;

abstract public class AbstractConductor {
  abstract protected void mainProcess(ConductorEntity entity);
  abstract protected void eventHandler(ConductorEntity entity, AbstractRun run);
  abstract protected void postProcess(ConductorEntity entity);
  abstract protected void suspendProcess(ConductorEntity entity);
  abstract public String defaultScriptName();
  abstract public void prepareConductor(Conductor conductor);

  private static HashMap<String, AbstractConductor> instanceMap = new HashMap<>();
  private static HashMap<ConductorEntity, AbstractConductor> runningInstance = new HashMap<>();

  public AbstractConductor() {
  }

  public void start(ConductorEntity entity) {
    (new Thread() {
      @Override
      public void run() {
        super.run();
        mainProcess(entity);
        return;
      }
    }).start();
  }

  public void eventHandle(ConductorEntity entity, AbstractRun run) {
    eventHandler(entity, run);
    if (! entity.getTrial().isRunning()) {
      postProcess(entity);
      entity.remove();
    }
  }

  public void hibernate(ConductorEntity entity) {
    suspendProcess(entity);
  }

  public static AbstractConductor getInstance(ConductorEntity entity) {
    if (runningInstance.containsKey(entity)) {
      return runningInstance.get(entity);
    }
    return getInstance(entity.getConductor().getConductorType());
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
