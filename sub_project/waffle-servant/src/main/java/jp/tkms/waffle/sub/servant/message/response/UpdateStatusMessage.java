package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;

public class UpdateStatusMessage extends AbstractResponseMessage {
  byte type;
  String id;
  boolean isFinished;
  int exitStatus;

  public UpdateStatusMessage() { }

  public UpdateStatusMessage(byte type, String id, boolean isFinished, int exitStatus) {
    this.type = type;
    this.id = id;
    this.isFinished = isFinished;
    this.exitStatus = exitStatus;
  }

  public UpdateStatusMessage(CollectStatusMessage collectStatusMessage) {
    this(collectStatusMessage.getType(), collectStatusMessage.getId(), false, -2);
  }

  public UpdateStatusMessage(CollectStatusMessage collectStatusMessage, int exitStatus) {
    this(collectStatusMessage.getType(), collectStatusMessage.getId(), true, exitStatus);
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

  public int getExitStatus() {
    return exitStatus;
  }
}
