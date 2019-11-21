package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

import java.time.LocalDateTime;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ConductorRun run);
  abstract protected void cycleProcess(ConductorRun run);

  @Override
  protected void mainProcess(ConductorRun run) {
    Trial trial = Trial.create(run.getProject(), run.getTrial(), LocalDateTime.now().toString());
    run.setTrial(trial);
    preProcess(run);
    cycleProcess(run);
  }

  @Override
  protected void eventHandler(ConductorRun run) {
    if (! run.getTrial().isRunning()) {
      cycleProcess(run);
    }
  }

}
