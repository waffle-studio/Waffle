package jp.tkms.waffle.script;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.script.ruby.RubyScriptProcessor;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.HashMap;

public abstract class ScriptProcessor {

  public enum ProcedureMode {
    START_OR_FINISHED_ALL,
    CONTAIN_FAULT,
    APPEALED
  };

  abstract public void processProcedure(ProcedureRun run, ProcedureMode mode, String script);
  abstract public String procedureTemplate();
  abstract public void processExtractor(AbstractSubmitter submitter, ExecutableRun run, String extractorName);
  abstract public String extractorTemplate();
  abstract public void processCollector(AbstractSubmitter submitter, ExecutableRun run, String collectorName);
  abstract public String collectorTemplate();
  abstract public String checkSyntax(Path scriptPath);

  private static HashMap<String, ScriptProcessor> instanceMap = new HashMap<>();

  public static final HashMap<String, String> CLASS_NAME_MAP = new HashMap<>() {
    {
      put(RubyScriptProcessor.EXTENSION, RubyScriptProcessor.class.getCanonicalName());
    }
  };

  public static ScriptProcessor getProcessor(String className) {
    ScriptProcessor processor = null;
    synchronized (instanceMap) {
      if (!instanceMap.containsKey(className)) {
        Class<ScriptProcessor> clazz = null;
        try {
          clazz = (Class<ScriptProcessor>) Class.forName(className);
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }

        Constructor<ScriptProcessor> constructor;
        try {
          constructor = clazz.getConstructor();
        } catch (SecurityException | NoSuchMethodException e) {
          ErrorLogMessage.issue(e);
          return null;
        }

        try {
          processor = constructor.newInstance();
        } catch (IllegalArgumentException | ReflectiveOperationException e) {
          ErrorLogMessage.issue(e);
          return null;
        }

        instanceMap.put(className, processor);
      } else {
        processor = instanceMap.get(className);
      }
    }
    return processor;
  }

  public static ScriptProcessor getProcessor(Path scriptPath) {
    return getProcessor(CLASS_NAME_MAP.get(scriptPath.toString().replaceFirst("^.*\\.", ".")));
  }
}
