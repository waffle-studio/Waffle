package jp.tkms.waffle.inspector;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.communicator.AbstractSubmitter;

public class Inspector extends Thread {
  public enum Mode {System, Normal}

  private Mode mode;
  private Computer computer;

  Inspector(Mode mode, Computer computer) {
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
        try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        waitCount += 1;
        if (Main.hibernateFlag) {
          break;
        }
      }
      if (Main.hibernateFlag) {
        break;
      }
      waitCount = 0;
      if (!Main.hibernateFlag) {
        if (submitter == null || !submitter.isConnected()) {
          InfoLogMessage.issue(computer, "will be reconnected");
          if (submitter != null) {
            submitter.close();
          }
          try {
            submitter = AbstractSubmitter.getInstance(mode, computer).connect();
          } catch (Exception e) {
            WarnLogMessage.issue(e.getMessage());
            submitter.close();
            InspectorMaster.removeInspector(getThreadName(mode, computer));
            InfoLogMessage.issue(computer, mode.name() + ") submitter closed");
            return;
          }
          if (!submitter.isConnected()) {
            WarnLogMessage.issue("Failed to connect to " + computer.getName());
            continue;
          }
        }
        try {
          submitter.checkSubmitted();
        } catch (FailedToControlRemoteException e) {
          submitter.close();
          WarnLogMessage.issue(computer, "was scanned with error");
          continue;
        }
        InfoLogMessage.issue(computer, "was scanned");
      }
    } while ((mode.equals(Mode.Normal) ? ExecutableRunTask.getList(computer).size() : SystemTask.getList(computer).size()) > 0);

    if (submitter != null) {
      submitter.close();
    }
    InspectorMaster.removeInspector(getThreadName(mode, computer));

    InfoLogMessage.issue(computer, mode.name() + " submitter closed");
    return;
  }

  static String getThreadName(Mode mode, Computer computer) {
    return mode.name() + ":" + computer.getName();
  }
}
