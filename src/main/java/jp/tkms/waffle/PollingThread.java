package jp.tkms.waffle;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.ComputerState;
import jp.tkms.waffle.submitter.AbstractSubmitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PollingThread extends Thread {
  private static final Map<String, PollingThread> threadMap = new HashMap<>();
  private static final HashSet<Computer> runningSubmitterSet = new HashSet<>();

  private Computer computer;

  public PollingThread(Computer computer) {
    super("Waffle_Polling(" + computer.getName() + ")");
    this.computer = computer;
  }

  @Override
  public void run() {
    InfoLogMessage.issue(computer, "submitter started");

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(computer).connect();

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
        runningSubmitterSet.add(computer);
      }
      if (submitter == null || !submitter.isConnected()) {
        if (submitter != null) {
          submitter.close();
        }
        try {
          InfoLogMessage.issue(computer, "will be scanned");
          submitter = AbstractSubmitter.getInstance(computer).connect();
        } catch (Exception e) {
          WarnLogMessage.issue(e);
          InfoLogMessage.issue(computer, "submitter closed");
          return;
        }
        if (! submitter.isConnected()) {
          WarnLogMessage.issue("Failed to connect to " + computer.getName());
          continue;
        }
      }
      try {
        submitter.pollingTask(computer);
      } catch (FailedToControlRemoteException e) {
        submitter.close();
        WarnLogMessage.issue(computer, "was scanned with error");
        continue;
      }
      InfoLogMessage.issue(computer, "was scanned");
      synchronized (runningSubmitterSet) {
        runningSubmitterSet.remove(computer);
        /*
        if (runningSubmitterSet.isEmpty()) {
          try {
            Main.jobStore.save();
          } catch (IOException e) {
            ErrorLogMessage.issue(e);
          }
        }
         */
      }
    } while (Job.getList(computer).size() > 0);

    if (submitter != null && submitter.isConnected()) {
      submitter.close();
    }
    threadMap.remove(computer.getName());

    InfoLogMessage.issue(computer, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Computer computer : Computer.getList()) {
        if (computer.getState().equals(ComputerState.Viable)) {
          if (!threadMap.containsKey(computer.getName()) && Job.hasJob(computer)) {
            computer.update();
            if (computer.getState().equals(ComputerState.Viable)) {
              PollingThread pollingThread = new PollingThread(computer);
              threadMap.put(computer.getName(), pollingThread);
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
