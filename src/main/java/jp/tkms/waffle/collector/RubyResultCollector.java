package jp.tkms.waffle.collector;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.Remote;
import jp.tkms.waffle.data.util.ResourceFile;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

public class RubyResultCollector extends AbstractResultCollector {
  @Override
  public void collect(AbstractSubmitter submitter, SimulatorRun run, String collectorName) {
    /*
    try {
      String json = submitter.getFileContents(run, collector.getContents().replaceAll("[\n\r\t]", ""));
      run.putResultsByJson(json);
    } catch (Exception e) { e.printStackTrace(); }
     */
    boolean failed;
    do {
      failed = false;
      try {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        try {
          container.runScriptlet(getInitScript(run));
          container.runScriptlet(run.getSimulator().getCollectorScript(collectorName));
          Remote remote = new Remote(run, submitter);
          container.callMethod(Ruby.newInstance().getCurrentContext(), "exec_result_collect", run, remote);
          container.terminate();
        } catch (EvalFailedException e) {
          container.terminate();
          BrowserMessage.addMessage("toastr.error('result_collect: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
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
    return "def result_collect(run, remote)\n" +
      "end";
  }

  private String getInitScript(SimulatorRun run) {
    return ResourceFile.getContents("/ruby_init.rb");
  }
}
