package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public class TestConductor extends AbstractConductor {
  @Override
  protected void mainProcess(ConductorRun run) {
    Conductor conductor = run.getConductor();
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      Run.create(conductor, run.getTrial(), simulator, Host.getList().get(0)).start();
    }
  }

  @Override
  protected void eventHandler(ConductorRun run) {

  }

  @Override
  protected void postProcess(ConductorRun run) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(Conductor conductor) {

  }
}
