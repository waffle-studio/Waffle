package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.Conductor;

public abstract class CycleConductor extends AbstractConductor {
  abstract void preProcess();
  abstract void cycleProcess();
  abstract void postProcess();

  @Override
  public void mainProcess(Conductor conductor) {
    preProcess();
  }


}
