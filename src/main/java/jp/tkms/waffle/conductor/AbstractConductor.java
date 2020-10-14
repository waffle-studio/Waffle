package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.AbstractRun;
import jp.tkms.waffle.data.ActorGroup;
import jp.tkms.waffle.data.ActorRun;
import jp.tkms.waffle.data.util.State;

import java.lang.reflect.Constructor;
import java.util.HashMap;

abstract public class AbstractConductor {
  abstract protected void mainProcess(ActorRun entity);
  abstract protected void eventHandler(ActorRun entity, AbstractRun run);
  abstract protected void finalizeProcess(ActorRun entity);
  abstract protected void suspendProcess(ActorRun entity);
  abstract public String defaultScriptName();
  abstract public void prepareConductor(ActorGroup conductor);

  private static HashMap<String, AbstractConductor> instanceMap = new HashMap<>();
  private static HashMap<ActorRun, AbstractConductor> runningInstance = new HashMap<>();

  public AbstractConductor() {
  }

  public void start(ActorRun conductorRun, boolean async) {
    Thread thread = new Thread() {
      @Override
      public void run() {
        super.run();
        mainProcess(conductorRun);
        if (! conductorRun.isRunning()) {
          finalizeProcess(conductorRun);
          conductorRun.finish();
        }
        return;
      }
    };
    thread.start();
    if (!async) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void eventHandle(ActorRun conductorRun, AbstractRun run) {
    eventHandler(conductorRun, run);
    if (! conductorRun.isRunning()) {
      if (run.getState().equals(State.Finished)) {
        finalizeProcess(conductorRun);
      } else {
        conductorRun.setState(State.Failed);
      }
      conductorRun.finish();
    }
  }

  public void hibernate(ActorRun entity) {
    suspendProcess(entity);
  }

  public static AbstractConductor getInstance(ActorRun entity) {
    if (runningInstance.containsKey(entity)) {
      return runningInstance.get(entity);
    }
    return getInstance(RubyConductor.class.getCanonicalName());
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
