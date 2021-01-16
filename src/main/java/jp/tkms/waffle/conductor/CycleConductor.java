package jp.tkms.waffle.conductor;


import jp.tkms.waffle.data.project.workspace.run.ConductorRun;

public abstract class CycleConductor extends AbstractConductor {
  abstract protected void preProcess(ConductorRun entity);

  @Override
  protected void mainProcess(ConductorRun entity) {
    preProcess(entity);
  }

}
