package jp.tkms.waffle.sub.vje;

import java.io.IOException;
import java.util.HashMap;

public class LocalExecutor extends AbstractExecutor {
  private HashMap<String, Thread> threadMap = new HashMap<>();

  public LocalExecutor() throws IOException {
    super();
  }

  @Override
  protected void jobAdded(String jobName) {
    super.jobAdded(jobName);

    threadMap.put(jobName, startJobThread(jobName));
  }

  @Override
  protected void jobRemoved(String jobName) {
    super.jobRemoved(jobName);

    Thread thread = threadMap.get(jobName);
    if (thread != null) {
      thread.interrupt();
    }
  }
}
