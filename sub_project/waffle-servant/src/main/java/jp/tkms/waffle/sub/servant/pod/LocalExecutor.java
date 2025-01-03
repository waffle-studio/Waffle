package jp.tkms.waffle.sub.servant.pod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LocalExecutor extends AbstractExecutor {
  private HashMap<String, Thread> threadMap = new HashMap<>();

  public LocalExecutor(int timeout, int marginTime) throws IOException {
    super(timeout, marginTime);
    //checkJobs();
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

  @Override
  protected void jobFinished(String jobName) {
    super.jobFinished(jobName);
    threadMap.remove(jobName);
  }
}
