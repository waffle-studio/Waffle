package jp.tkms.waffle.data.log;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.WaffleException;

public class WarnLogMessage extends LogMessage {
  public WarnLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new WarnLogMessage(message).printMessage();
  }

  public static void issue(Throwable e) {
    new WarnLogMessage(getStackTrace(e)).printMessage();
  }

  public static void issue(SimulatorRun run, Throwable e) {
    new WarnLogMessage("Run(" + run.getProject().getId() + "/" + run.getId() + ") " + getStackTrace(e)).printMessage();
  }

  public static void issue(WaffleException e) {
    new WarnLogMessage(e.getMessage()).printMessage();
  }

  public static void issue(SimulatorRun run, String message) {
    new WarnLogMessage("Run(" + run.getProject().getId() + "/" + run.getId() + ") " + message).printMessage();
  }

  public static void issue(Host host, String message) {
    new WarnLogMessage("Host(" + host.getName() + ") " + message).printMessage();
  }

  public static void issue(SimulatorRun run, WaffleException e) {
    issue(run, e.getMessage());
  }
}
