package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ActorRun;
import jp.tkms.waffle.data.project.conductor.ActorGroup;

public class EmptyConductor extends CycleConductor {
  @Override
  protected void preProcess(ActorRun entity) {

  }

  @Override
  protected void eventHandler(ActorRun entity, AbstractRun run) {

  }

  @Override
  protected void finalizeProcess(ActorRun entity) {

  }

  @Override
  protected void suspendProcess(ActorRun entity) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(ActorGroup conductor) {

  }
}
