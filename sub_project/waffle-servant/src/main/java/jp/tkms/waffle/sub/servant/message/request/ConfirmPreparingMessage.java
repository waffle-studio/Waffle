package jp.tkms.waffle.sub.servant.message.request;

public class ConfirmPreparingMessage extends JobMessage {

  public ConfirmPreparingMessage() {}

  public ConfirmPreparingMessage(byte type, String id) {
    super(type, id);
  }
}
