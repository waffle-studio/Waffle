package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public class TestConductor extends AbstractConductor {
  @Override
  protected void mainProcess(ConductorEntity entity) {
    Conductor conductor = entity.getConductor();
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      Run.create(entity, simulator, Host.getList().get(0)).start();
    }
  }

  @Override
  protected void eventHandler(ConductorEntity entity) {

  }

  @Override
  protected void postProcess(ConductorEntity entity) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(Conductor conductor) {

  }
}
