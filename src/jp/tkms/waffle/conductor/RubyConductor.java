package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;

public class RubyConductor extends AbstractConductor {
  @Override
  public void process(Conductor conductor) {
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      Trials trials = Trials.getRootInstance(conductor.getProject());
      Run run =  Run.create(conductor, trials, simulator, Host.getList().get(0));
      run.start();
    }
  }
}
