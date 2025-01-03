package jp.tkms.waffle.collector;

import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.Remote;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.script.ruby.util.RubyScript;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;

public class RubyResultCollector extends AbstractResultCollector {
  @Override
  public void collect(AbstractSubmitter submitter, ExecutableRun run, String collectorName) {
    RubyScript.process((container) -> {
      try {
        container.runScriptlet(run.getExecutable().getCollectorScript(collectorName));
        Remote remote = new Remote(run, submitter);
        container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_result_collect", run, remote);
      } catch (EvalFailedException e) {
        WarnLogMessage.issue(e);
      }
    });
  }

  @Override
  public String contentsTemplate() {
    return "def result_collect(run, remote)\n" +
      "end";
  }

  private String getInitScript(ExecutableRun run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
