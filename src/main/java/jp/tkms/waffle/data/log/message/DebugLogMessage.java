package jp.tkms.waffle.data.log.message;

public class DebugLogMessage extends LogMessage {
  public DebugLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new DebugLogMessage(message).printMessage();
  }

  public static void issue(Throwable e) {
    new DebugLogMessage(getStackTrace(e)).printMessage();
  }
}
