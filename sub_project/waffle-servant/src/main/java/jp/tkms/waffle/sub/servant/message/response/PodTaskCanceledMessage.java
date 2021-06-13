package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;

public class PodTaskCanceledMessage extends AbstractResponseMessage {
  byte type;
  String id;

  public PodTaskCanceledMessage() { }

  public PodTaskCanceledMessage(byte type, String id) {
    this.type = type;
    this.id = id;
  }

  public PodTaskCanceledMessage(JobMessage jobMessage) {
    this(jobMessage.getType(), jobMessage.getId());
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
