package jp.tkms.waffle;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
      try { sleep(submitter.getPollingInterval() * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
      if (submitter == null || !submitter.isConnected()) {
        if (submitter != null) {
          submitter.close();
        }
        try {
          submitter = AbstractSubmitter.getInstance(host).connect();
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          InfoLogMessage.issue(host, "submitter closed");
          return;
        }
        if (! submitter.isConnected()) {
          WarnLogMessage.issue("Failed to connect to " + host.getName());
          continue;
        }
      }
      try {
        submitter.pollingTask(host);
      } catch (FailedToControlRemoteException e) {
        submitter.close();
        WarnLogMessage.issue(host, "was scanned with error");
        continue;
      }
      InfoLogMessage.issue(host, "was scanned");
    } while (Job.getList(host).size() > 0);

    if (submitter != null && submitter.isConnected()) {
      submitter.close();
    }
    threadMap.remove(host.getId());

    InfoLogMessage.issue(host, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Host host : Host.getList()) {
        if (host.getState().equals(HostState.Viable)) {
          if (!threadMap.containsKey(host.getId()) && Job.getList(host).size() > 0) {
            host.update();
            if (host.getState().equals(HostState.Viable)) {
              PollingThread pollingThread = new PollingThread(host);
              threadMap.put(host.getId(), pollingThread);
              pollingThread.start();
            }
          }
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
