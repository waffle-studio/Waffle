package jp.tkms.waffle.data.util;

import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

import java.util.function.Consumer;

public class RubyScript {
  private static Integer runningCount = 0;

  public static boolean hasRunning() {
    synchronized (runningCount) {
      return (runningCount > 0);
    }
  }

  public static void process(Consumer<ScriptingContainer> process) {
    boolean failed;
    do {
      synchronized (runningCount) {
        runningCount += 1;
      }
      failed = false;
      ScriptingContainer container = null;
      try {
        container = new ScriptingContainer(LocalContextScope.THREADSAFE);
        try {
          container.runScriptlet(RubyConductor.getInitScript());
          process.accept(container);
        } catch (EvalFailedException e) {
          ErrorLogMessage.issue(e);
        }
        container.terminate();
        container = null;
      } catch (SystemCallError | LoadError e) {
        failed = true;
        if (! e.getMessage().matches("Unknown error")) {
          failed = false;
        }
        WarnLogMessage.issue(e);
        try { Thread.sleep(1000); } catch (InterruptedException ex) { }
      } finally {
        if (container != null) {
          container.terminate();
        }
        synchronized (runningCount) {
          runningCount -= 1;
        }
      }
    } while (failed);
  }
}
