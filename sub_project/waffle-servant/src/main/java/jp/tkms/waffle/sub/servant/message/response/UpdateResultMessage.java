package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;

public class UpdateResultMessage extends AbstractResponseMessage {
  byte type;
  String id;
  String key;
  String value;

  public UpdateResultMessage() { }

  public UpdateResultMessage(byte type, String id, String key, String value) {
    this.type = type;
    this.id = id;
    this.key = key;
    this.value = value;
  }

  public UpdateResultMessage(CollectStatusMessage collectStatusMessage, String key, String value) {
    this(collectStatusMessage.getType(), collectStatusMessage.getId(), key, value);
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
