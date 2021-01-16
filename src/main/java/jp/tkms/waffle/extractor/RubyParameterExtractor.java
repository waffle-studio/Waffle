package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.data.util.RubyScript;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;

public class RubyParameterExtractor extends AbstractParameterExtractor {
  @Override
  public void extract(AbstractSubmitter submitter, ExecutableRun run, String extractorName) {
    RubyScript.process((container) -> {
      try {
        container.runScriptlet(run.getExecutable().getExtractorScript(extractorName));
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_parameter_extract", run);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
    });
  }

  @Override
  public String contentsTemplate() {
    return "def parameter_extract(run)\n" +
      "end";
  }

  private String getInitScript(ExecutableRun run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
