package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;

import java.lang.reflect.Constructor;
import java.util.HashMap;

abstract public class AbstractConductor {
  abstract protected void mainProcess(ProcedureRun entity);
  abstract protected void eventHandler(ProcedureRun entity, AbstractRun run);
  abstract protected void finalizeProcess(ProcedureRun entity);
  abstract protected void suspendProcess(ProcedureRun entity);
  abstract public String defaultScriptName();
  abstract public void prepareConductor(Conductor conductor);

  //private static HashMap<String, AbstractConductor> instanceMap = new HashMap<>();
  //private static HashMap<ConductorRun, AbstractConductor> runningInstance = new HashMap<>();

  public AbstractConductor() {
  }

  public void start(ConductorRun conductorRun, boolean async) {
    Thread thread = new Thread() {
      @Override
      public void run() {
        super.run();
        mainProcess(conductorRun);
        /*
        if (! conductorRun.isRunning()) {
          finalizeProcess(conductorRun);
          conductorRun.finish();
        }
         */
        //TODO:: conductor
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

  public void eventHandle(ConductorRun conductorRun, AbstractRun run) {
    eventHandler(conductorRun, run);
    /*
    if (! conductorRun.isRunning()) {
      if (run.getState().equals(State.Finished)) {
        finalizeProcess(conductorRun);
      } else {
        conductorRun.setState(State.Failed);
      }
      conductorRun.finish();
    }
     */
    //TODO:
  }

  public void hibernate(ConductorRun entity) {
    suspendProcess(entity);
  }

  public static AbstractConductor getInstance(ConductorRun entity) {
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
