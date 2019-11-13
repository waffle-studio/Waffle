package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public class TestConductor extends AbstractConductor {
  @Override
  public void mainProcess(Conductor conductor) {
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      Trial trial = Trial.getRootInstance(conductor.getProject());
      Run run =  Run.create(conductor, trial, simulator, Host.getList().get(0));
      run.start();
    }
  }
}
