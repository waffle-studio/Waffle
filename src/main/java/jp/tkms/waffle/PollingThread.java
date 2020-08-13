package jp.tkms.waffle;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

public class PollingThread extends Thread {
  private static final Map<String, PollingThread> threadMap = new HashMap<>();
  private static final HashSet<Host> runningSubmitterSet = new HashSet<>();

  private Host host;

  public PollingThread(Host host) {
    super("Waffle_Polling(" + host.getName() + ")");
    this.host = host;
  }

  @Override
  public void run() {
    InfoLogMessage.issue(host, "submitter started");

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(host).connect();

    int waitCount = submitter.getPollingInterval() - 1;
    do {
      while (waitCount < submitter.getPollingInterval()) {
        if (Main.hibernateFlag) {
          submitter.hibernate();
          break;
        }
        try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        waitCount += 1;
      }
      if (Main.hibernateFlag) {
        submitter.hibernate();
        break;
      }
      waitCount = 0;

      synchronized (runningSubmitterSet) {
        runningSubmitterSet.add(host);
      }
      if (submitter == null || !submitter.isConnected()) {
        if (submitter != null) {
          submitter.close();
        }
        try {
          InfoLogMessage.issue(host, "will be scanned");
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
      synchronized (runningSubmitterSet) {
        runningSubmitterSet.remove(host);
        if (runningSubmitterSet.isEmpty()) {
          try {
            Main.jobStore.save();
          } catch (IOException e) {
            ErrorLogMessage.issue(e);
          }
        }
      }
    } while (Job.getList(host).size() > 0);

    if (submitter != null && submitter.isConnected()) {
      submitter.close();
    }
    threadMap.remove(host.getName());

    InfoLogMessage.issue(host, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Host host : Host.getList()) {
        if (host.getState().equals(HostState.Viable)) {
          if (!threadMap.containsKey(host.getName()) && Job.hasJob(host)) {
            host.update();
            if (host.getState().equals(HostState.Viable)) {
              PollingThread pollingThread = new PollingThread(host);
              threadMap.put(host.getName(), pollingThread);
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
