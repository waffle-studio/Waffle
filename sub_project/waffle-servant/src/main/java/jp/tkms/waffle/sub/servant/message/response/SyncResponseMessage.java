package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.SyncRequestMessage;

public class SyncResponseMessage extends AbstractResponseMessage {
  long value;

  public SyncResponseMessage() { }

  public SyncResponseMessage(SyncRequestMessage message) {
    this.value = message.getValue();
  }

  public long getValue() {
    return value;
  }
}
