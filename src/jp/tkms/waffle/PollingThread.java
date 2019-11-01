package jp.tkms.waffle;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollingThread extends Thread {
  private static Map<String, PollingThread> threadMap = new HashMap<>();

  private Host host;

  public PollingThread(Host host) {
    this.host = host;
  }

  @Override
  public void run() {
    int pollingTime = host.getPollingInterval() * 1000;

    ArrayList<Job> jobList = Job.getList(host);
    while (jobList.size() > 0) {
      AbstractSubmitter submitter = AbstractSubmitter.getInstance(host);

      for (Job job : jobList) {
        Run run = job.getRun();
        switch (run.getState()) {
          case Created:
            submitter.submit(job);
            break;
          case Submitted:
          case Running:
          case Finished:
            submitter.update(job);
            break;
        }
      }

      submitter.close();

      try { Thread.sleep(pollingTime); } catch (InterruptedException e) { e.printStackTrace(); }
      jobList = Job.getList(host);
    }

    threadMap.remove(host.getId());
  }

  public static void startup() {
    for (Host host : Host.getList()) {
      if (!threadMap.containsKey(host.getId()) && Job.getList(host).size() > 0) {
        PollingThread pollingThread = new PollingThread(host);
        threadMap.put(host.getId(), pollingThread);
        pollingThread.start();
      }
    }
  }
}
