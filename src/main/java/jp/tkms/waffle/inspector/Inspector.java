package jp.tkms.waffle.inspector;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.communicator.AbstractSubmitter;

import java.util.concurrent.TimeUnit;

public class Inspector extends Thread {
  public enum Mode {System, Normal}

  protected Mode mode;
  protected Computer computer;
  protected int waitCount;

  Inspector(Mode mode, Computer computer) {
    super("Waffle_Polling(" + getThreadName(mode, computer) + ")");
    this.mode = mode;
    this.computer = computer;
  }

  @Override
  public void run() {
    InfoLogMessage.issue(computer, "submitter started");

    AbstractSubmitter submitter = AbstractSubmitter.getInstance(mode, computer).connect();

    waitCount = (submitter.getPollingInterval() - 1) * 10;
    do {
      while (waitCount < submitter.getPollingInterval() * 10) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          break;
        }
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
