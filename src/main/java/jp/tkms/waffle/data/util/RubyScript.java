package jp.tkms.waffle.data.util;

import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.SystemCallError;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class RubyScript {
  private static Integer runningCount = 0;
  private static ConcurrentLinkedDeque<ScriptingContainer> containersQueue = new ConcurrentLinkedDeque<>();
  private static ConcurrentLinkedDeque<Long> containersTimestampQueue = new ConcurrentLinkedDeque<>();

  public static boolean hasRunning() {
    synchronized (runningCount) {
      return (runningCount > 0);
    }
  }

  private static ScriptingContainer getScriptingContainer() {
    ScriptingContainer scriptingContainer = null;
    synchronized (containersTimestampQueue) {
      scriptingContainer = containersQueue.pollLast();
      containersTimestampQueue.pollLast();
    }
    if (scriptingContainer != null) {
      return scriptingContainer;
    }
    //return new ScriptingContainer(LocalContextScope.THREADSAFE);
    return new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);
  }

  private static void releaseScriptingContainer(ScriptingContainer scriptingContainer) {
    scriptingContainer.clear();
    synchronized (containersTimestampQueue) {
      containersQueue.offerLast(scriptingContainer);
      containersTimestampQueue.offerLast(System.currentTimeMillis());
      if (containersTimestampQueue.size() > 1) {
        try {
          while (containersTimestampQueue.peekFirst() + 10000 < System.currentTimeMillis()) {
            ScriptingContainer container = containersQueue.pollFirst();
            containersTimestampQueue.pollFirst();
            container.terminate();
          }
        } catch (Exception e) {}
      }
    }
  }

  public static void processOld(Consumer<ScriptingContainer> process) {
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

  public static void process(Consumer<ScriptingContainer> process) {
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
          container.runScriptlet(RubyConductor.getInitScript());
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
  }

  public static String debugReport() {
    return RubyScript.class.getSimpleName() + " : cacheSize=" + containersQueue.size() + ", runningCount=" + runningCount;
  }
}
