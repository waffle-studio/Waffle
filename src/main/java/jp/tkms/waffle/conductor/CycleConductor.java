package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.project.workspace.run.ActorRun;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ActorRun entity);

  @Override
  protected void mainProcess(ActorRun entity) {
    preProcess(entity);
  }

}
