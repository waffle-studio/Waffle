package jp.tkms.waffle;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
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

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(host).connect();

    do {
      try { sleep(host.getPollingInterval() * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
      if (! submitter.isConnected()) {
        submitter.close();
        submitter = AbstractSubmitter.getInstance(host).connect();
        if (! submitter.isConnected()) {
          WarnLogMessage.issue("Failed to connect to " + host.getName());
          continue;
        }
      }
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
      try {
        submitter.pollingTask(host);
      } catch (FailedToControlRemoteException e) {
        submitter.close();
        WarnLogMessage.issue(host, "was scanned with error");
        continue;
      }
      InfoLogMessage.issue(host, "was scanned");
      //host = Host.getInstance(host.getId());
      System.gc();
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
    } while (Job.getList(host).size() > 0);

    submitter.close();
    threadMap.remove(host.getId());

    InfoLogMessage.issue(host, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Host host : Host.getList()) {
        if (!threadMap.containsKey(host.getId()) && Job.getList(host).size() > 0) {
          host.update();
          PollingThread pollingThread = new PollingThread(host);
          threadMap.put(host.getId(), pollingThread);
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
