package jp.tkms.waffle.extractor;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.ParameterExtractor;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

public class RubyParameterExtractor extends AbstractParameterExtractor {
  @Override
  public void extract(Run run, ParameterExtractor extractor, AbstractSubmitter submitter) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(extractor.getScript());
      container.callMethod(Ruby.newInstance().getCurrentContext(), "parameter_extract", run);
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('parameter_extract: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  public String contentsTemplate() {
    return "def parameter_extract(run)\n" +
      "end";
  }

  private String getInitScript(Run run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
