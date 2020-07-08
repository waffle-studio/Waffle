package jp.tkms.waffle;

import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.submitter.AbstractSubmitter;

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
    InfoLogMessage.issue(host, "submitter started");

    do {
      try { sleep(host.getPollingInterval() * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
      AbstractSubmitter submitter = AbstractSubmitter.getInstance(host).connect();
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
      submitter.pollingTask(host);
      submitter.close();
      InfoLogMessage.issue(host, "was scanned");
      host = Host.getInstanceByName(host.getName());
      System.gc();
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
    } while (Job.getList(host).size() > 0);

    threadMap.remove(host.getName());

    InfoLogMessage.issue(host, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Host host : Host.getList()) {
        if (!threadMap.containsKey(host.getName()) && Job.getList(host).size() > 0) {
          host.update();
          PollingThread pollingThread = new PollingThread(host);
          threadMap.put(host.getName(), pollingThread);
          pollingThread.start();
        }
      }
    }
  }

  synchronized public static void waitForShutdown() {
    while (! threadMap.isEmpty()) {
      try {
        sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
