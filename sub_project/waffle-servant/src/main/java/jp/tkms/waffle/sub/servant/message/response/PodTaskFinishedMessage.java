package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.JobMessage;

public class PodTaskFinishedMessage extends AbstractResponseMessage {
  byte type;
  String id;

  public PodTaskFinishedMessage() { }

  public PodTaskFinishedMessage(byte type, String id) {
    this.type = type;
    this.id = id;
  }

  public PodTaskFinishedMessage(JobMessage jobMessage) {
    this(jobMessage.getType(), jobMessage.getId());
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
