package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.Ruby;
import org.jruby.embed.*;
import org.jruby.exceptions.SyntaxError;

import java.io.*;
import java.nio.file.Path;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun conductorRun) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript());
      container.runScriptlet(getConductorTemplateScript());
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('conductor_script: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
    try {
      container.runScriptlet(PathType.ABSOLUTE, conductorRun.getConductor().getScriptPath().toString());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_conductor_script", conductorRun);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
      conductorRun.appendErrorNote(e.getMessage());
      BrowserMessage.addMessage("toastr.error('conductor_script: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
    container.terminate();
  }

  @Override
  protected void eventHandler(ConductorRun conductorRun, AbstractRun run) {
    if (run instanceof SimulatorRun) {
      SimulatorRun simulatorRun = (SimulatorRun)run;
      for (String script : simulatorRun.getFinalizers()) {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        try {
          container.runScriptlet(getInitScript());
          container.runScriptlet(getListenerTemplateScript());
          container.runScriptlet(script);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_script", conductorRun, simulatorRun);
        } catch (Exception e) {
          System.err.println(e.getMessage());
          conductorRun.appendErrorNote(e.getMessage());
          BrowserMessage.addMessage("toastr.error('simulator_finalizer_script: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
        }
        container.terminate();
      }
    }
  }

  @Override
  protected void finalizeProcess(ConductorRun conductorRun) {
    //TODO: do refactor
    ConductorRun parent = conductorRun.getParent();
    if (parent == null) {
      parent = conductorRun;
    }

    for (String script : conductorRun.getFinalizers()) {
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
      try {
        container.runScriptlet(getInitScript());
        container.runScriptlet(getListenerTemplateScript());
        container.runScriptlet(script);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_script", parent, conductorRun);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        conductorRun.appendErrorNote(e.getMessage());
        BrowserMessage.addMessage("toastr.error('conductor_finalizer_script: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
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

      filewriter.write(getConductorTemplateScript());
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }

  private static String getConductorTemplateScript() {
    return ResourceFile.getContents("/ruby_conductor_template.rb");
  }

  public static String getListenerTemplateScript() {
    return ResourceFile.getContents("/ruby_listener_template.rb");
  }

  public static String checkSyntax(Path scriptPath) {
    String error = "";
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.parse(PathType.ABSOLUTE, scriptPath.toString());
    } catch (ParseFailedException e) {
        error = e.getMessage().replaceFirst("^.*\\.rb:", "");
    }
    container.terminate();
    return error;
  }
}
