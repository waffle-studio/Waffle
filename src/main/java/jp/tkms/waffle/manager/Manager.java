package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.internal.guard.ProcedureRunGuard;
import jp.tkms.waffle.data.internal.guard.ProcedureRunGuardStore;
import jp.tkms.waffle.data.internal.guard.ValueGuard;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Manager {
  private Workspace workspace;
  private ExecutorService executorService;

  Manager(Workspace workspace) {
    this.workspace = workspace;
    this.executorService = Executors.newSingleThreadScheduledExecutor();
  }

  public void signalUpdated(ProcedureRunGuardStore store, AbstractRun run) {
    executorService.submit(() -> updateProcess(store, run));
  }

  public void signalFinished(ProcedureRunGuardStore store, AbstractRun run) {
    executorService.submit(() -> finishProcess(store, run));
  }

  private boolean isConformed(ValueGuard valueGuard, Object value) {
    return (new Filter(valueGuard)).apply(value);
  }

  private void deactivateAndTryRun(ProcedureRunGuardStore store, ProcedureRunGuard guard) {
    store.remove(guard);
    ProcedureRun procedureRun = guard.getProcedureRun();
    procedureRun.deactivateGuard(guard.getTargetRunPath());
    if (procedureRun.getActiveGuardList().size() <= 0) {
      procedureRun.run();
    }
  }

  private void updateProcess(ProcedureRunGuardStore store, AbstractRun run) {
    process(store, run, (guard) -> {
      if (guard.isValueGuard()) {
        ProcedureRun procedureRun = guard.getProcedureRun();
        for (String guardString : procedureRun.getActiveGuardList()) {
          try {
            ValueGuard valueGuard = new ValueGuard(guardString);
            if (run instanceof ConductorRun) {
              ConductorRun conductorRun = (ConductorRun) run;
              if (isConformed(valueGuard, conductorRun.getVariable(valueGuard.getKey()))) {
                deactivateAndTryRun(store, guard);
                break;
              }
            } else if (run instanceof ExecutableRun) {
              ExecutableRun executableRun = (ExecutableRun) run;
              if (isConformed(valueGuard, executableRun.getResult(valueGuard.getKey()))) {
                deactivateAndTryRun(store, guard);
                break;
              }
            }
          } catch (Exception e) {
            ErrorLogMessage.issue(e);
          }
        }
      }
    });
  }

  private void finishProcess(ProcedureRunGuardStore store, AbstractRun run) {
    process(store, run, (guard) -> {
      if (!guard.isValueGuard()) {
        deactivateAndTryRun(store, guard);
      }
    });

    ConductorRun parentConductorRun = run.getParentConductorRun();
    if (parentConductorRun != null) {
      executorService.submit(parentConductorRun::updateRunningStatus);
      //parentConductorRun.updateRunningStatus();
    }
  }

  private void process(ProcedureRunGuardStore store, AbstractRun run, Consumer<ProcedureRunGuard> consumer) {
    ArrayList<ProcedureRunGuard> list = store.getList(run);
    for (ProcedureRunGuard guard : list) {
      consumer.accept(guard);
    }
  }
}
