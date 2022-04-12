package jp.tkms.waffle.communicator;

import jp.tkms.waffle.communicator.annotation.CommunicatorDescription;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;

import java.util.ArrayList;

@CommunicatorDescription("SSH (limited by thread and memory)")
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
