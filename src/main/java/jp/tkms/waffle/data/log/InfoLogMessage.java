package jp.tkms.waffle.data.log;

import jp.tkms.waffle.data.Computer;
import jp.tkms.waffle.data.SimulatorRun;

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
    new InfoLogMessage("Host(" + computer.getName() + ") " + message).printMessage();
  }
}
