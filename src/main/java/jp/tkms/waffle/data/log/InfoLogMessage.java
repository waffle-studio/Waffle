package jp.tkms.waffle.data.log;

public class InfoLogMessage extends LogMessage {
  public InfoLogMessage(String message) {
    super(message);
  }

  public static void issue(String message) {
    new InfoLogMessage(message).printMessage();
  }
}
