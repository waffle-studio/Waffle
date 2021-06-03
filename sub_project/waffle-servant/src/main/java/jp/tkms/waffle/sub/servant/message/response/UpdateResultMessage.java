package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;

public class UpdateResultMessage extends AbstractResponseMessage {
  byte type;
  String id;
  String name;
  String value;

  public UpdateResultMessage() { }

  public UpdateResultMessage(byte type, String id, String name, String value) {
    this.type = type;
    this.id = id;
    this.name = name;
    this.value = value;
  }

  public UpdateResultMessage(CollectStatusMessage collectStatusMessage, String name, String value) {
    this(collectStatusMessage.getType(), collectStatusMessage.getId(), name, value);
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
