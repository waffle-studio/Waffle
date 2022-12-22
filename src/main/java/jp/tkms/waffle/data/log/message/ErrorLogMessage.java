package jp.tkms.waffle.data.log.message;

import jp.tkms.waffle.data.computer.Computer;

public class ErrorLogMessage extends LogMessage {
  public ErrorLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new ErrorLogMessage(message).printMessage();
  }

  public static void issue(Throwable e) {
    new ErrorLogMessage(getStackTrace(e)).printMessage();
  }

  public static void issue(Computer computer, String message) {
    new ErrorLogMessage("Computer(" + computer.getName() + ") " + message).printMessage();
  }
}
