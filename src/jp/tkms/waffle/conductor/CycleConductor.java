package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

import java.util.ArrayList;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ConductorRun run);
  abstract protected void cycleProcess(ConductorRun run);

  @Override
  protected void mainProcess(ConductorRun run) {
    preProcess(run);
    cycleProcess(run);
  }

  @Override
  protected void eventHandler(ConductorRun run) {
    cycleProcess(run);
  }

}
