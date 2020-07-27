package jp.tkms.waffle.conductor;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.State;
import org.jruby.Ruby;
import org.jruby.embed.*;

import java.nio.file.Path;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(Actor actor) {
    //actor.processMessage(null);
  }

  @Override
  protected void eventHandler(Actor conductorRun, AbstractRun run) {
    if (run instanceof SimulatorRun) {
      if (((SimulatorRun) run).getState().equals(State.Finished)) {
        SimulatorRun simulatorRun = (SimulatorRun) run;
        for (String script : simulatorRun.getFinalizers()) {
          ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
          try {
            container.runScriptlet(getInitScript());
            container.runScriptlet(getListenerTemplateScript());
            container.runScriptlet(script);
            container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_listener_script", conductorRun, simulatorRun);
          } catch (Exception e) {
            WarnLogMessage.issue(e);
            conductorRun.appendErrorNote(e.getMessage());
            BrowserMessage.addMessage("toastr.error('simulator_finalizer_script: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
          }
          container.terminate();
        }
      }
    }
  }

  @Override
  protected void finalizeProcess(Actor conductorRun) {
    //TODO: do refactor
    Actor parent = conductorRun.getParentActor();
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
        WarnLogMessage.issue(e);
        conductorRun.appendErrorNote(e.getMessage());
        BrowserMessage.addMessage("toastr.error('conductor_finalizer_script: " + e.getMessage().replaceAll("['\"\n]", "\"") + "');");
      }
      container.terminate();
    }
  }

  @Override
  protected void suspendProcess(Actor entity) {

  }

  @Override
  public String defaultScriptName() {
    return ActorGroup.KEY_REPRESENTATIVE_ACTOR + Constants.EXT_RUBY;
  }

  @Override
  public void prepareConductor(ActorGroup conductor) {
  }

  public static String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }

  public static String getConductorTemplateScript() {
    return ResourceFile.getContents("/ruby_conductor_template.rb");
  }

  public static String getListenerTemplateScript() {
    return ResourceFile.getContents("/ruby_actor_template.rb");
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
