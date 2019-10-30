package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Trials;

public class TestConductor extends AbstractConductor {
  @Override
  public void process(Conductor conductor) {
    Trials trials = Trials.getRootInstance(conductor.getProject());
    Run run = Run.create(conductor, Host.getList().get(0), trials);
  }
}
