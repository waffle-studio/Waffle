package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.internal.step.ProcedureRunGuard;
import jp.tkms.waffle.data.internal.step.ProcedureRunGuardStore;
import jp.tkms.waffle.data.project.workspace.Workspace;
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

  public static Manager getManager(Workspace workspace) {
    synchronized (managerMap) {
      Manager manager = managerMap.get(workspace.getLocalPath().toString());
      if (manager == null) {
        manager = new Manager(workspace);
      }
      return manager;
    }
  }

  public static void update(Workspace workspace) {
    getManager(workspace).update();
  }

  public static void register(ProcedureRun procedureRun) {
    for (String guard : procedureRun.getActiveGuardList()) {
      procedureRunGuardStore.register(new ProcedureRunGuard(procedureRun, guard.split(" ", 2)[0]));
    }
  }
}
