package jp.tkms.waffle.conductor.module;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class RubyConductorModule {
  static RubyConductorModule instance = new RubyConductorModule();

  public static RubyConductorModule getInstance() {
    return instance;
  }

  public void registerDefaultParameters(ConductorRun conductorRun, ConductorModule module, String moduleInstanceName) {
    try {
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_register_default_parameters", conductorRun, moduleInstanceName);
    } catch (EvalFailedException e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public boolean cycleProcess(ScriptingContainer container, ConductorRun conductorRun, ConductorModule module, String moduleInstanceName, AbstractRun run) {
    boolean result = true;
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      result = container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_module_cycle_process", Arrays.asList(conductorRun, moduleInstanceName, run).toArray(), Boolean.class);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
    return result;
  }

  public void postCycleProcess(ScriptingContainer container, ConductorRun conductorRun, ConductorModule module, String moduleInstanceName, AbstractRun run) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_cycle_process", conductorRun, moduleInstanceName, run);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void finalizeProcess(ScriptingContainer container, ConductorRun conductorRun, ConductorModule module, String moduleInstanceName) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_post_process", conductorRun, moduleInstanceName);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  protected void suspendProcess(ConductorRun entity) {

  }

  public String defaultScriptName() {
    return "main.rb";
  }


  public static void prepareModule(ConductorModule module) {
    try {
      FileWriter filewriter = new FileWriter(module.getScriptPath().toFile());

      filewriter.write(getTemplateScript());
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String getTemplateScript() {
    return ResourceFile.getContents("/ruby_conductor_template.rb");
  }
}
