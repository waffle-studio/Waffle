package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.communicator.AbstractSubmitter;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public abstract class AbstractParameterExtractor {

  abstract public void extract(AbstractSubmitter submitter, ExecutableRun run, String extractorName);
  abstract public String contentsTemplate();

  public AbstractParameterExtractor() {
  }

  public static HashMap<String, AbstractParameterExtractor> instanceMap = new HashMap<>();

  public static AbstractParameterExtractor getInstance(String className) {
    AbstractParameterExtractor conductor = null;

    if (! instanceMap.containsKey(className)) {
      Class<AbstractParameterExtractor> clazz = null;
      try {
        clazz = (Class<AbstractParameterExtractor>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }

      Constructor<AbstractParameterExtractor> constructor;
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
