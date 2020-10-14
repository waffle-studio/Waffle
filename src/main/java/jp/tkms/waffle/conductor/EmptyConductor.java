package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.AbstractRun;
import jp.tkms.waffle.data.ActorRun;
import jp.tkms.waffle.data.ActorGroup;

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
