package jp.tkms.waffle;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import spark.Spark;

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

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(host).connect();
    do {
      submitter.pollingTask(host);
      try { Thread.sleep(pollingTime); } catch (InterruptedException e) { e.printStackTrace(); }
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
    } while (Job.getList(host).size() > 0);
    submitter.close();

    threadMap.remove(host.getId());
  }

  synchronized public static void startup() {
    for (Host host : Host.getList()) {
      if (!threadMap.containsKey(host.getId()) && Job.getList(host).size() > 0) {
        PollingThread pollingThread = new PollingThread(host);
        threadMap.put(host.getId(), pollingThread);
        pollingThread.start();
      }
    }
  }
}
