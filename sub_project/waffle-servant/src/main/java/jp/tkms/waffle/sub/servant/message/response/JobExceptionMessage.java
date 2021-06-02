package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;

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

  public JobExceptionMessage(JobMessage jobMessage, String message) {
    this(jobMessage.getType(), jobMessage.getId(), message);
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
