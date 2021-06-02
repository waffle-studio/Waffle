package jp.tkms.waffle.sub.servant.message.request;

import jp.tkms.waffle.sub.servant.message.request.AbstractRequestMessage;

public abstract class JobMessage extends AbstractRequestMessage {
  byte type;
  String id;

  public JobMessage() { }

  public JobMessage(byte type, String id) {
    this.type = type;
    this.id = id;
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
