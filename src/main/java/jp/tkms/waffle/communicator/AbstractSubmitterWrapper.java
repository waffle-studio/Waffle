package jp.tkms.waffle.communicator;

import jp.tkms.utils.concurrent.LockByKey;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.Envelope;

public abstract class AbstractSubmitterWrapper extends AbstractSubmitter {
  public AbstractSubmitterWrapper(Computer computer) {
    super(computer);
  }

  @Override
  public void cancel(Envelope envelope, AbstractTask job) throws RunNotFoundException, FailedToControlRemoteException {
    try (LockByKey lock = LockByKey.acquire(job.getHexCode())) {
      job.setState(State.Aborted);
    }
  }
}
