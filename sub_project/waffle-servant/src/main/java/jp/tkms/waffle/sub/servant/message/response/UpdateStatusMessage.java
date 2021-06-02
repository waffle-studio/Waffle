package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;

public class UpdateStatusMessage extends AbstractResponseMessage {
  byte type;
  String id;
  boolean isFinished;

  public UpdateStatusMessage() { }

  public UpdateStatusMessage(byte type, String id, boolean isFinished) {
    this.type = type;
    this.id = id;
    this.isFinished = isFinished;
  }

  public UpdateStatusMessage(CollectStatusMessage collectStatusMessage, boolean isFinished) {
    this(collectStatusMessage.getType(), collectStatusMessage.getId(), isFinished);
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public boolean isFinished() {
    return isFinished;
  }
}
