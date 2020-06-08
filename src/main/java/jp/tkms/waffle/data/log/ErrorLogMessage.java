package jp.tkms.waffle.data.log;

public class ErrorLogMessage extends LogMessage {
  public ErrorLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new ErrorLogMessage(message).printMessage();
  }
}
