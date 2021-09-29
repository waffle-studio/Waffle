package jp.tkms.waffle.script.ruby;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.Remote;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.script.ruby.util.RubyScript;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.*;

import java.nio.file.Path;
import java.util.ArrayList;

public class RubyScriptProcessor extends ScriptProcessor {
  public static final String EXTENSION = Constants.EXT_RUBY;

  @Override
  public void processProcedure(ProcedureRun run, ProcedureMode mode, AbstractRun caller, String script, ArrayList<Object> arguments) {
    RubyScript.process((container) -> {
      try {
        container.runScriptlet(procedureTemplate());
        container.runScriptlet(script);
        if (mode.equals(ProcedureMode.START_OR_FINISHED_ALL)) {
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_procedure_when_start_or_finished_all", run, caller);
        } else if (mode.equals(ProcedureMode.CONTAIN_FAULT)) {
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_procedure_when_contain_fault", run, caller);
        } else if (mode.equals(ProcedureMode.RESULT_UPDATED)) {
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_procedure_when_result_updated", run, caller, arguments.get(0), arguments.get(1));
        } else if (mode.equals(ProcedureMode.APPEALED)) {
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_procedure_when_appealed", run, caller, arguments.get(0), arguments.get(1));
        }
    } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
    });
  }

  @Override
  public String procedureTemplate() {
    return
      "def procedure_when_start_or_finished_all(this, caller)\n" +
      "end\n" +
      "\n" +
      "def procedure_when_contain_fault(this, caller)\n" +
      "    #procedure_when_start_or_finished_all(this, caller)\n" +
      "end\n" +
      "\n" +
      "def procedure_when_result_updated(this, caller, name, value)\n" +
      "end\n" +
      "\n" +
      "def procedure_when_appealed(this, caller, appealer, message)\n" +
      "end\n";
  }

  @Override
  public void processExtractor(AbstractSubmitter submitter, ExecutableRun run, String extractorName) {
    RubyScript.process((container) -> {
      try {
        container.runScriptlet(extractorTemplate());
        container.runScriptlet(run.getExecutable().getExtractorScript(extractorName));
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_parameter_extract", run);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
    });
  }

  @Override
  public String extractorTemplate() {
    return "def parameter_extract(this)\n" +
      "end\n";
  }

  @Override
  public void processCollector(AbstractSubmitter submitter, ExecutableRun run, String collectorName) {

    RubyScript.process((container) -> {
      try {
        container.runScriptlet(collectorTemplate());
        container.runScriptlet(run.getExecutable().getCollectorScript(collectorName));
        Remote remote = new Remote(run, submitter);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_result_collect", run, remote);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
    });
  }

  @Override
  public String collectorTemplate() {
    return "def result_collect(this, remote)\n" +
      "end\n";
  }

  @Override
  public String checkSyntax(Path scriptPath) {
    String error = "";
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.parse(PathType.ABSOLUTE, scriptPath.toString());
    } catch (ParseFailedException e) {
      error = e.getMessage().replaceFirst("^.*?:", "");
    }
    container.terminate();
    return error;
  }
}
