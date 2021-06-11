package jp.tkms.waffle.script.ruby.util;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.ResourceFile;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class RubyScript {
  private static Integer runningCount = 0;

  public static boolean hasRunning() {
    synchronized (runningCount) {
      return (runningCount > 0);
    }
  }

  public static void process(Consumer<ScriptingContainer> process) {
    ScriptingContainerWrapper wrapper = new ScriptingContainerWrapper(process);
    wrapper.start();
    try {
      synchronized (wrapper) {
        wrapper.wait();
      }
    } catch (InterruptedException e) {
      ErrorLogMessage.issue(e);
    }

    /*
    boolean failed;
    do {
      synchronized (runningCount) {
        runningCount += 1;
      }
      failed = false;
      ScriptingContainer container = null;
      try {
        container = getScriptingContainer();
        try {
          container.runScriptlet(getInitScript());
          process.accept(container);
        } catch (EvalFailedException e) {
          ErrorLogMessage.issue(e);
        }
      } catch (SystemCallError | LoadError e) {
        failed = true;
        if (! e.getMessage().matches("Unknown error")) {
          failed = false;
        }
        WarnLogMessage.issue(e);
        try { Thread.sleep(1000); } catch (InterruptedException ex) { }
      } finally {
        releaseScriptingContainer(container);
        synchronized (runningCount) {
          runningCount -= 1;
        }
      }
    } while (failed);
     */
  }

  public static String debugReport() {
    return RubyScript.class.getSimpleName() + " : runningCount=" + runningCount;
  }

  public static String getInitScript() {
    return ResourceFile.getContents("/ruby_init.rb");
  }


  static class ScriptingContainerWrapper extends Thread {
    Consumer<ScriptingContainer> process;

    public ScriptingContainerWrapper(Consumer<ScriptingContainer> process) {
      super(ScriptingContainerWrapper.class.getSimpleName());
      this.process = process;
    }

    @Override
    public void run() {
      boolean failed;
      do {
        synchronized (runningCount) {
          runningCount += 1;
        }
        failed = false;
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.TRANSIENT);
        try {
          try {
            container.runScriptlet(getInitScript());
            process.accept(container);
          } catch (EvalFailedException e) {
            ErrorLogMessage.issue(e);
          }
        } catch (SystemCallError | LoadError e) {
          failed = true;
          if (! e.getMessage().matches("Unknown error")) {
            failed = false;
          }
          WarnLogMessage.issue(e);
          try { Thread.sleep(1000); } catch (InterruptedException ex) { }
        } finally {
          container.terminate();
          container = null;
          synchronized (runningCount) {
            runningCount -= 1;
          }
        }
      } while (failed);
    }
  }
}
