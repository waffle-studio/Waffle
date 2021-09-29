package jp.tkms.waffle.inspector;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.*;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.util.ComputerState;
import jp.tkms.waffle.data.util.WaffleId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InspectorMaster {
  private static boolean isAvailable = true;
  private static final Map<String, Inspector> inspectorMap = new HashMap<>();
  private static Thread masterThread = null;

  private static SystemTaskStore systemTaskStore = null;
  private static ExecutableRunTaskStore executableRunTaskStore = null;

  static void removeInspector(String name) {
    synchronized (inspectorMap) {
      inspectorMap.remove(name);
    }
  }

  public static void removeSystemTask(WaffleId id) {
    systemTaskStore.remove(id);
  }

  public static void registerSystemTask(SystemTask job) {
    systemTaskStore.register(job);
  }

  public static SystemTask getSystemTask(WaffleId id) {
    return systemTaskStore.getTask(id);
  }

  public static ArrayList<SystemTask> getSystemTaskList() {
    return systemTaskStore.getList();
  }

  public static ArrayList<AbstractTask> getSystemTaskList(Computer computer) {
    return new ArrayList<>(systemTaskStore.getList(computer));
  }

  public static void removeExecutableRunTask(WaffleId id) {
    executableRunTaskStore.remove(id);
  }

  public static void registerExecutableRunTask(ExecutableRunTask job) {
    executableRunTaskStore.register(job);
  }

  public static ExecutableRunTask getExecutableRunTask(WaffleId id) {
    return executableRunTaskStore.getTask(id);
  }

  public static ArrayList<ExecutableRunTask> getExecutableRunTaskList() {
    return executableRunTaskStore.getList();
  }

  public static ArrayList<AbstractTask> getExecutableRunTaskList(Computer computer) {
    return new ArrayList<>(executableRunTaskStore.getList(computer));
  }

  public static void startup() {
    synchronized (inspectorMap) {

      try {
        systemTaskStore = SystemTaskStore.load();
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }

      try {
        executableRunTaskStore = ExecutableRunTaskStore.load();
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }

      // periodic wakeup thread
      if (masterThread == null && isAvailable) {
        masterThread = new Thread("Inspector_Master") {
          @Override
          public void run() {
            while (isAvailable) {
              try {
                TimeUnit.SECONDS.sleep(5);
              } catch (InterruptedException e) {
                break;
              }
              forceCheck();
            }
            masterThread = null;
          }
        };
        masterThread.start();
      }
    }

    forceCheck();
  }

  public static void forceCheck() {
    synchronized (inspectorMap) {
      // startup all inspectors
      if (!Main.hibernateFlag) {
        for (Computer computer : Computer.getList()) {
          if (computer.getState().equals(ComputerState.Viable)) {
            if (!inspectorMap.containsKey(Inspector.getThreadName(Inspector.Mode.Normal, computer)) && ExecutableRunTask.hasJob(computer)) {
              computer.update();
              if (computer.getState().equals(ComputerState.Viable)) {
                Inspector inspector = new Inspector(Inspector.Mode.Normal, computer);
                inspectorMap.put(Inspector.getThreadName(Inspector.Mode.Normal, computer), inspector);
                inspector.start();
              }
            }
            if (!inspectorMap.containsKey(Inspector.getThreadName(Inspector.Mode.System, computer)) && SystemTask.hasJob(computer)) {
              computer.update();
              if (computer.getState().equals(ComputerState.Viable)) {
                Inspector inspector = new Inspector(Inspector.Mode.System, computer);
                inspectorMap.put(Inspector.getThreadName(Inspector.Mode.System, computer), inspector);
                inspector.start();
              }
            }
          }
        }
      }

    }
  }

  public static void waitForShutdown() {
    isAvailable = false;

    // stop periodic wakeup thread
    synchronized (inspectorMap) {
      if (masterThread != null) {
        masterThread.interrupt();
      }
    }

    // wait for stopping whole inspectors
    while (! inspectorMap.isEmpty()) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
