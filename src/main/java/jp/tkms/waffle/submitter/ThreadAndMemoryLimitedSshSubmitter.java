package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.exception.RunNotFoundException;

import java.util.ArrayList;

public class ThreadAndMemoryLimitedSshSubmitter extends JobNumberLimitedSshSubmitter {
  public ThreadAndMemoryLimitedSshSubmitter(Computer computer) {
    super(computer);
  }

  @Override
  protected boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<ComputerTask> list) {
    double thread = (next == null ? 0.0: next.getRequiredThread());
    thread += list.stream().mapToDouble(o->o.getRequiredThread()).sum();
    double memory = (next == null ? 0.0: next.getRequiredMemory());
    memory += list.stream().mapToDouble(o->o.getRequiredMemory()).sum();

    return (thread <= computer.getMaximumNumberOfThreads() && memory <= computer.getAllocableMemorySize());
  }
}
