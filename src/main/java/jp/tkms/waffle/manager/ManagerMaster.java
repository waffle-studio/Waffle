package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.internal.guard.ProcedureRunGuard;
import jp.tkms.waffle.data.internal.guard.ProcedureRunGuardStore;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;

import java.util.HashMap;
import java.util.Map;

public class ManagerMaster {
  private static final Map<String, Manager> managerMap = new HashMap<>();
  private static ProcedureRunGuardStore procedureRunGuardStore = null;

  public static void startup() {
    synchronized (managerMap) {
      if (procedureRunGuardStore == null) {
        procedureRunGuardStore = ProcedureRunGuardStore.load();
      }
    }
  }

  public static void reset() {
    synchronized (managerMap) {
      boolean isRemoved = false;
      if (procedureRunGuardStore != null) {
        for (ProcedureRunGuard guard : procedureRunGuardStore.getList()) {
          procedureRunGuardStore.remove(guard);
          isRemoved = true;
        }
      }
      if (isRemoved) {
        managerMap.clear();
        InfoLogMessage.issue("Remove all guards");
      }
    }
  }

  public static Manager getManager(Workspace workspace) {
    synchronized (managerMap) {
      Manager manager = managerMap.get(workspace.getLocalPath().toString());
      if (manager == null) {
        manager = new Manager(workspace);
        managerMap.put(workspace.getLocalPath().toString(), manager);
      }
      return manager;
    }
  }

  public static void signalUpdated(AbstractRun run) {
    Workspace workspace = run.getWorkspace();
    getManager(run.getWorkspace()).signalUpdated(procedureRunGuardStore, run);
    tryManagerClosing(workspace);
  }

  public static void signalFinished(AbstractRun run) {
    Workspace workspace = run.getWorkspace();
    getManager(workspace).signalFinished(procedureRunGuardStore, run);
    tryManagerClosing(workspace);
  }

  public static void register(ProcedureRun procedureRun) {
    for (String guard : procedureRun.getActiveGuardList()) {
      procedureRunGuardStore.register(ProcedureRunGuard.factory(procedureRun, guard));
    }
  }

  public static void tryManagerClosing(Workspace workspace) {
    if (workspace.isFinished()) {
      synchronized (managerMap) {
        managerMap.remove(workspace.getLocalPath().toString());
        System.out.println("WS_CLOSED: " + workspace.getLocalPath().toString());
      }
    }
  }
}
