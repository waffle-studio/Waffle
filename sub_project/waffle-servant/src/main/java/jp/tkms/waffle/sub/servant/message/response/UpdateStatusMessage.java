package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;
import jp.tkms.waffle.sub.servant.message.request.JobMessage;

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

  public UpdateStatusMessage(JobMessage jobMessage) {
    this(jobMessage.getType(), jobMessage.getId(), false, -2);
  }

  public UpdateStatusMessage(JobMessage jobMessage, int exitStatus) {
    this(jobMessage.getType(), jobMessage.getId(), true, exitStatus);
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
