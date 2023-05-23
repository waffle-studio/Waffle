package jp.tkms.waffle.sub.servant.message.request;

import jp.tkms.waffle.sub.servant.GetValueCommand;
import jp.tkms.waffle.sub.servant.message.response.SendValueMessage;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SyncRequestMessage extends AbstractRequestMessage {
  long value;

  public SyncRequestMessage() {}

  public SyncRequestMessage(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }
}
