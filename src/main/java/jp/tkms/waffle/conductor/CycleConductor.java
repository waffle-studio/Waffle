package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(Actor entity);

  @Override
  protected void mainProcess(Actor entity) {
    preProcess(entity);
  }

}
