package jp.tkms.waffle.data.log;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.SimulatorRun;

public class InfoLogMessage extends LogMessage {
  public InfoLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new InfoLogMessage(message).printMessage();
  }

  public static void issue(SimulatorRun run, String message) {
    new InfoLogMessage("Run(" + run.getProject().getId() + "/" + run.getId() + ") " + message).printMessage();
  }

  public static void issue(Host host, String message) {
    new InfoLogMessage("Host(" + host.getName() + ") " + message).printMessage();
  }
}
