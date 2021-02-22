package jp.tkms.waffle;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.SystemTaskJob;
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

  public enum Mode {System, Normal}

  private Mode mode;
  private Computer computer;

  public PollingThread(Mode mode, Computer computer) {
    super("Waffle_Polling(" + getThreadName(mode, computer) + ")");
    this.mode = mode;
    this.computer = computer;
  }

  @Override
  public void run() {
    InfoLogMessage.issue(computer, "submitter started");

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(mode, computer).connect();

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

      if (submitter == null || !submitter.isConnected()) {
        InfoLogMessage.issue(computer, "will be reconnected");
        if (submitter != null) {
          submitter.close();
        }
        try {
          submitter = AbstractSubmitter.getInstance(mode, computer).connect();
        } catch (Exception e) {
          WarnLogMessage.issue(e.getMessage());
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
    } while ((mode.equals(Mode.Normal) ? ExecutableRunJob.getList(computer).size() : SystemTaskJob.getList(computer).size()) > 0);

    if (submitter != null) {
      submitter.close();
    }
    threadMap.remove(getThreadName(mode, computer));

    InfoLogMessage.issue(computer, "submitter closed");
  }

  synchronized public static void startup() {
    if (!Main.hibernateFlag) {
      for (Computer computer : Computer.getList()) {
        if (computer.getState().equals(ComputerState.Viable)) {
          if (!threadMap.containsKey(getThreadName(Mode.Normal, computer)) && ExecutableRunJob.hasJob(computer)) {
            computer.update();
            if (computer.getState().equals(ComputerState.Viable)) {
              PollingThread pollingThread = new PollingThread(Mode.Normal, computer);
              threadMap.put(getThreadName(Mode.Normal, computer), pollingThread);
              pollingThread.start();
            }
          }
          if (!threadMap.containsKey(getThreadName(Mode.System, computer)) && SystemTaskJob.hasJob(computer)) {
            computer.update();
            if (computer.getState().equals(ComputerState.Viable)) {
              PollingThread pollingThread = new PollingThread(Mode.System, computer);
              threadMap.put(getThreadName(Mode.System, computer), pollingThread);
              pollingThread.start();
            }
          }
        }
      }
    }
  }

  private static String getThreadName(Mode mode, Computer computer) {
    return mode.name() + ":" + computer.getName();
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
