package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.exception.RunNotFoundException;

import java.util.ArrayList;

public class ThreadAndMemoryLimitedSshSubmitter extends JobNumberLimitedSshSubmitter {
  public ThreadAndMemoryLimitedSshSubmitter(Computer computer) {
    super(computer);
  }

  @Override
  protected boolean isSubmittable(Computer computer, AbstractJob next, ArrayList<AbstractJob> list) {
    ComputerTask nextRun = null;
    try {
      if (next != null) {
        nextRun = next.getRun();
      }
    } catch (RunNotFoundException e) {
    }
    double thread = (nextRun == null ? 0.0: nextRun.getRequiredThread());
    thread += list.stream().mapToDouble(o->o.getRequiredThread()).sum();
    double memory = (nextRun == null ? 0.0: nextRun.getRequiredMemory());
    memory += list.stream().mapToDouble(o->o.getRequiredMemory()).sum();

    return (thread <= computer.getMaximumNumberOfThreads() && memory <= computer.getAllocableMemorySize());
  }
}
