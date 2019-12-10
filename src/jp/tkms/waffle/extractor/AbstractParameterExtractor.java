package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.ParameterExtractor;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.joda.time.base.AbstractPeriod;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public abstract class AbstractParameterExtractor {

  abstract public void extract(Run run, ParameterExtractor extractor, AbstractSubmitter submitter);
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
