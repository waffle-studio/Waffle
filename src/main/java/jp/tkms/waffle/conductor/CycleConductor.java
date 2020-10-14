package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ActorRun entity);

  @Override
  protected void mainProcess(ActorRun entity) {
    preProcess(entity);
  }

}
