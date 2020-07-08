package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.AbstractRun;
import jp.tkms.waffle.data.Actor;
import jp.tkms.waffle.data.ActorGroup;

public class EmptyConductor extends CycleConductor {
  @Override
  protected void preProcess(Actor entity) {

  }

  @Override
  protected void eventHandler(Actor entity, AbstractRun run) {

  }

  @Override
  protected void finalizeProcess(Actor entity) {

  }

  @Override
  protected void suspendProcess(Actor entity) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(ActorGroup conductor) {

  }
}
