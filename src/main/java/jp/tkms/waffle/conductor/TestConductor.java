package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public class TestConductor extends AbstractConductor {
  @Override
  protected void mainProcess(ConductorRun entity) {
    Conductor conductor = entity.getConductor();
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      SimulatorRun.create(entity, simulator, Host.getInstanceByName(entity.getVariable("host").toString())).start();
    }
  }

  @Override
  protected void eventHandler(ConductorRun entity, AbstractRun run) {

  }

  @Override
  protected void finalizeProcess(ConductorRun entity) {

  }

  @Override
  protected void suspendProcess(ConductorRun entity) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(Conductor conductor) {
    conductor.setDefaultVariables("{'host':'LOCAL'}");
  }
}
