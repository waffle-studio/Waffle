package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ConductorRun entity);

  @Override
  protected void mainProcess(ConductorRun entity) {
    Trial trial = Trial.create(entity.getProject(), entity.getTrial(), entity.getName());
    entity.setTrial(trial);
    preProcess(entity);
  }

}
