package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.*;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("exec_pre_process(ConductorRun.find(\"" + run.getProject().getId() + "\",\"" + run.getId() + "\"))");
    } catch (EvalFailedException e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void cycleProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("exec_cycle_process(ConductorRun.find(\"" + run.getProject().getId() + "\",\"" + run.getId() + "\"))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void postProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("exec_post_process(ConductorRun.find(\"" + run.getProject().getId() + "\",\"" + run.getId() + "\"))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
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
        "def pre_process(registry, store, crun)\n" +
          "end\n" +
          "\n" +
          "def cycle_process(registry, store, crun)\n" +
          "end\n" +
          "\n" +
          "def post_process(registry, store, crun)\n" +
          "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getInitScript(ConductorRun run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
