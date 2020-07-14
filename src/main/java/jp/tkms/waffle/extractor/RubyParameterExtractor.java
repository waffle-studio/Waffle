package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.RubySystemCallError;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

public class RubyParameterExtractor extends AbstractParameterExtractor {
  @Override
  public void extract(AbstractSubmitter submitter, SimulatorRun run, String extractorName) {
    boolean failed;
    do {
      failed = false;
      try {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        try {
          container.runScriptlet(getInitScript(run));
          container.runScriptlet(run.getSimulator().getExtractorScript(extractorName));
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_parameter_extract", run);
          container.terminate();
        } catch (EvalFailedException e) {
          container.terminate();
          WarnLogMessage.issue(e);
        }
      } catch (SystemCallError | LoadError e) {
        failed = true;
        WarnLogMessage.issue(e);
        try { Thread.sleep(1000); } catch (InterruptedException ex) { }
      }
    } while (failed);
  }

  @Override
  public String contentsTemplate() {
    return "def parameter_extract(run)\n" +
      "end";
  }

  private String getInitScript(SimulatorRun run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
