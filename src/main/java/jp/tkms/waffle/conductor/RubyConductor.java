package jp.tkms.waffle.conductor;

import jp.tkms.waffle.conductor.module.RubyConductorModule;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.*;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun conductorRun) {
    eventHandler(conductorRun, null);
  }

  @Override
  protected void eventHandler(ConductorRun conductorRun, AbstractRun run) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, conductorRun.getConductor().getScriptPath().toString());

      container.callMethod(Ruby.newInstance().getCurrentContext(), "register_modules", conductorRun);

      boolean result = true;
      for (String key : conductorRun.getModuleKeyList()) {
        result = RubyConductorModule.getInstance().cycleProcess(container, conductorRun, conductorRun.getModule(key), key, run);
        if (!result) { break; }
      }

      if (result) {
        container.runScriptlet(getTemplateScript());
        container.runScriptlet(PathType.ABSOLUTE, conductorRun.getConductor().getScriptPath().toString());
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_cycle_process", conductorRun, run);
      }

      for (String key : conductorRun.getModuleKeyList()) {
        RubyConductorModule.getInstance().postCycleProcess(container, conductorRun, conductorRun.getModule(key), key, run);
      }

      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, conductorRun.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_cycle_process", conductorRun, run);

      container.terminate();
    } catch (EvalFailedException e) {
      container.terminate();
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  protected void finalizeProcess(ConductorRun entity) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());

      container.callMethod(Ruby.newInstance().getCurrentContext(), "register_modules", entity);

      boolean result = true;
      for (String key : entity.getModuleKeyList()) {
        result = RubyConductorModule.getInstance().postProcess(container, module, entity);
        if (!result) { break; }
      }

      if (result) {
        container.runScriptlet(getTemplateScript());
        container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_process", entity);
      }

      for (ConductorModule module : entity.getModuleList()) {
        RubyConductorModule.getInstance().postPostProcess(container, module, entity);
      }

      container.runScriptlet(getTemplateScript());
      container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_post_process", entity);

      container.terminate();
    } catch (EvalFailedException e) {
      container.terminate();
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void suspendProcess(ConductorRun entity) {

  }

  @Override
  public String defaultScriptName() {
    return "main.rb";
  }

  @Override
  public void prepareConductor(Conductor conductor) {
    try {
      FileWriter filewriter = new FileWriter(conductor.getScriptPath().toFile());

      filewriter.write(
        "def register_modules(hub)\n" +
          "#    entity.register_module(\"ModuleName\")\n" +
          "end\n" +
          "\n" + getTemplateScript());
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }

  private static String getTemplateScript() {
    return ResourceFile.getContents("/ruby_conductor_template.rb");
  }
}
