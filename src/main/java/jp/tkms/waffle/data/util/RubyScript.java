package jp.tkms.waffle.data.util;

import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

public class RubyScript {
  public static void process(Process process) {
    boolean failed;
    do {
      failed = false;
      try {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        try {
          container.runScriptlet(RubyConductor.getInitScript());
          process.run(container);
        } catch (EvalFailedException e) {
          ErrorLogMessage.issue(e);
        }
        container.terminate();
      } catch (SystemCallError | LoadError e) {
        failed = true;
        if (! e.getMessage().matches("Unknown error")) {
          failed = false;
        }
        WarnLogMessage.issue(e);
        try { Thread.sleep(1000); } catch (InterruptedException ex) { }
      }
    } while (failed);
  }

  @FunctionalInterface
  public interface Process {
    void run(ScriptingContainer container);
  }
}
