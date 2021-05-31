package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;

public class JobExceptionMessage extends AbstractResponseMessage {
  byte type;
  String id;
  String message;

  public JobExceptionMessage() { }

  public JobExceptionMessage(byte type, String id, String message) {
    this.type = type;
    this.id = id;
    this.message = message;
  }

  public JobExceptionMessage(SubmitJobMessage submitJobMessage, String message) {
    this(submitJobMessage.getType(), submitJobMessage.getId(), message);
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public String getMessage() {
    return message;
  }
}
