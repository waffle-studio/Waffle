package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;

public class ExceptionMessage extends AbstractResponseMessage {
  String message;

  public ExceptionMessage() { }

  public ExceptionMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
