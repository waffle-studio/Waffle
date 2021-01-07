package jp.tkms.waffle.collector;

import jp.tkms.waffle.data.project.workspace.run.SimulatorRun;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.lang.reflect.Constructor;
import java.util.HashMap;

abstract public class AbstractResultCollector {

  abstract public void collect(AbstractSubmitter submitter, SimulatorRun run, String collectorName);
  abstract public String contentsTemplate();

  public AbstractResultCollector() {
  }

  public static HashMap<String, AbstractResultCollector> instanceMap = new HashMap<>();

  public static AbstractResultCollector getInstance(String className) {
    AbstractResultCollector conductor = null;

    if (! instanceMap.containsKey(className)) {
      Class<AbstractResultCollector> clazz = null;
      try {
        clazz = (Class<AbstractResultCollector>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      Constructor<AbstractResultCollector> constructor;
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
