package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

public class RubyParameterExtractor extends AbstractParameterExtractor {
  @Override
  public void extract(AbstractSubmitter submitter, SimulatorRun run, String extractorName) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(run.getSimulator().getExtractorScript(extractorName));
      container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_parameter_extract", run);
      container.terminate();
    } catch (EvalFailedException e) {
      container.terminate();
      BrowserMessage.addMessage("toastr.error('parameter_extract: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
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
