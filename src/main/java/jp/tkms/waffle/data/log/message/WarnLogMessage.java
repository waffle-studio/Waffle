package jp.tkms.waffle.data.log.message;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.exception.WaffleException;

public class WarnLogMessage extends LogMessage {
  public WarnLogMessage(String message) {
    super(message);
    new Throwable().printStackTrace();
  }

  public static void issue(String message) {
    new WarnLogMessage(message).printMessage();
  }

  public static void issue(Throwable e) {
    new WarnLogMessage(getStackTrace(e)).printMessage();
  }

  public static void issue(ComputerTask run, Throwable e) {
    new WarnLogMessage("Run(" + run.getLocalPath().toString() + ") " + getStackTrace(e)).printMessage();
  }

  public static void issue(WaffleException e) {
    new WarnLogMessage(getStackTrace(e)).printMessage();
  }

  public static void issue(ComputerTask run, String message) {
    new WarnLogMessage("Run(" + run.getLocalPath().toString() + ") " + message).printMessage();
  }

  public static void issue(Computer computer, String message) {
    new WarnLogMessage("Computer(" + computer.getName() + ") " + message).printMessage();
  }

  public static void issue(Computer computer, Throwable e) {
    issue(computer, getStackTrace(e));
  }

  public static void issue(ComputerTask run, WaffleException e) {
    issue(run, e.getMessage());
  }
}
