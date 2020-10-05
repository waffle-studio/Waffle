package jp.tkms.waffle.sub.vje;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LocalExecutor extends AbstractExecutor {
  private HashMap<String, Thread> threadMap = new HashMap<>();

  public LocalExecutor(int waitTime, int hesitationTime) throws IOException {
    super(waitTime, hesitationTime);
    checkJobs();
  }

  @Override
  public void shutdown() {
    super.shutdown();

    threadMap.forEach( (k, t) -> {
      t.interrupt();
    });
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
