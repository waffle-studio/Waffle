package jp.tkms.waffle.data.log.message;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.workspace.run.SimulatorRun;

public class InfoLogMessage extends LogMessage {
  public InfoLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new InfoLogMessage(message).printMessage();
  }

  public static void issue(SimulatorRun run, String message) {
    new InfoLogMessage("Run(" + run.getProject().getName() + "/" + run.getId() + ") " + message).printMessage();
  }

  public static void issue(Computer computer, String message) {
    new InfoLogMessage("Computer(" + computer.getName() + ") " + message).printMessage();
  }
}
