package jp.tkms.waffle.sub.servant.message.response;

public class UpdatePodStatusMessage extends AbstractResponseMessage {
  public static final byte RUNNING = 0;
  public static final byte LOCKED = 1;
  public static final byte FINISHED = 2;

  String id;
  byte state;

  public UpdatePodStatusMessage() { }

  public UpdatePodStatusMessage(String id, byte state) {
    this.id = id;
    this.state = state;
  }

  public String getId() {
    return id;
  }

  public byte getState() {
    return state;
  }
}
