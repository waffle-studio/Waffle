package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;

public class JobCanceledMessage extends AbstractResponseMessage {
  byte type;
  String id;

  public JobCanceledMessage() { }

  public JobCanceledMessage(byte type, String id) {
    this.type = type;
    this.id = id;
  }

  public JobCanceledMessage(CancelJobMessage cancelJobMessage) {
    this(cancelJobMessage.getType(), cancelJobMessage.getId());
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
