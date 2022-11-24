package jp.tkms.waffle.script;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.HasLocalPath;
import jp.tkms.waffle.data.project.workspace.convertor.WorkspaceConvertorRun;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.StringKeyHashMap;
import jp.tkms.waffle.script.ruby.RubyScriptProcessor;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.script.wnj.WaffleNodeJsonScriptProcessor;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class ScriptProcessor {

  abstract public String checkSyntax(Path scriptPath);
  abstract public void processProcedure(ProcedureRun run, StringKeyHashMap<HasLocalPath> referable, String script, ArrayList<Object> arguments);
  abstract public String procedureTemplate();
  abstract public void processExtractor(AbstractSubmitter submitter, ExecutableRun run, String extractorName);
  abstract public String extractorTemplate();
  abstract public void processCollector(AbstractSubmitter submitter, ExecutableRun run, String collectorName);
  abstract public String collectorTemplate();
  abstract public void processConvertor(WorkspaceConvertorRun run);
  abstract public String convertorTemplate();

  private static HashMap<String, ScriptProcessor> instanceMap = new HashMap<>();

  public static final HashMap<String, String> CLASS_NAME_MAP = new HashMap<>() {
    {
      put(RubyScriptProcessor.EXTENSION, RubyScriptProcessor.class.getCanonicalName());
      //put(WaffleNodeJsonScriptProcessor.EXTENSION, WaffleNodeJsonScriptProcessor.class.getCanonicalName());
    }
  };

  public static String getDescription(String className) {
    return ((ProcessorDescription)getProcessor(className).getClass().getAnnotation(ProcessorDescription.class)).value();
  }

  public void processProcedure(ProcedureRun run, StringKeyHashMap<HasLocalPath> referable, String script) {
    processProcedure(run, referable, script, null);
  }

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
    return getProcessor(CLASS_NAME_MAP.get(getExtension(scriptPath)));
  }

  public static String getExtension(Path scriptPath) {
    return scriptPath.toString().replaceFirst("^.*\\.", ".");
  }
}
