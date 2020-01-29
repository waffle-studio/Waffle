package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.json.JSONObject;

import java.io.*;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorEntity entity) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_pre_process", entity);
      container.terminate();
    } catch (EvalFailedException e) {
      e.printStackTrace();
      container.terminate();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void cycleProcess(ConductorEntity entity) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_cycle_process", entity);
      container.terminate();
    } catch (EvalFailedException e) {
      container.terminate();
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void postProcess(ConductorEntity entity) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(PathType.ABSOLUTE, entity.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_post_process", entity);
      container.terminate();
    } catch (EvalFailedException e) {
      container.terminate();
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void suspendProcess(ConductorEntity entity) {

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
        "def pre_process(entity, store, registry)\n" +
          "end\n" +
          "\n" +
          "def cycle_process(entity, store, registry)\n" +
          "end\n" +
          "\n" +
          "def post_process(entity, store, registry)\n" +
          "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
