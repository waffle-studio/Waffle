package jp.tkms.waffle.data.log;

public class WarnLogMessage extends LogMessage {
  public WarnLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new WarnLogMessage(message).printMessage();
  }

  public static void issue(Exception e) {
    new WarnLogMessage(getStackTrace(e)).printMessage();
  }
}
