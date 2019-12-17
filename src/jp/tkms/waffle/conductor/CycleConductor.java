package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ConductorEntity entity);
  abstract protected void cycleProcess(ConductorEntity entity);

  @Override
  protected void mainProcess(ConductorEntity entity) {
    Trial trial = Trial.create(entity.getProject(), entity.getTrial(), entity.getName());
    entity.setTrial(trial);
    preProcess(entity);
    cycleProcess(entity);
  }

  @Override
  protected void eventHandler(ConductorEntity entity) {
    if (! entity.getTrial().isRunning()) {
      cycleProcess(entity);
    }
  }

}
