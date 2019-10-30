package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.Conductor;

abstract public class AbstractConductor {
  abstract public void processo(Conductor conductor);

  public void run(Conductor conductor) {
    processo(conductor);
  }
}
