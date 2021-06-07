package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.computer.Computer;

public abstract class AbstractSubmitterWrapper extends AbstractSubmitter {
  public AbstractSubmitterWrapper(Computer computer) {
    super(computer);
  }
}
