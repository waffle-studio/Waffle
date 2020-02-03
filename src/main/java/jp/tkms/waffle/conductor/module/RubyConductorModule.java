package jp.tkms.waffle.conductor.module;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.FileWriter;
import java.io.IOException;

public class RubyConductorModule {
  static RubyConductorModule instance = new RubyConductorModule();

  public static RubyConductorModule getInstance() {
    return instance;
  }

  public void preProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_pre_process", entity);
    } catch (EvalFailedException e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void postPreProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_pre_process", entity);
    } catch (EvalFailedException e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void cycleProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity, AbstractRun run) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_cycle_process", entity, run);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void postCycleProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity, AbstractRun run) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_cycle_process", entity, run);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void postProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_process", entity);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  public void postPostProcess(ScriptingContainer container, ConductorModule module, ConductorEntity entity) {
    try {
      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, module.getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_post_process", entity);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
      throw e;
    }
  }

  protected void suspendProcess(ConductorEntity entity) {

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
