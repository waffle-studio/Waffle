package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.sub.servant.message.response.ExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.SyncResponseMessage;

import java.io.IOException;
import java.util.ArrayList;

public class SyncResponseMessageProcessor extends ResponseProcessor<SyncResponseMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<SyncResponseMessage> messageList) throws ClassNotFoundException, IOException {
    submitter.setRemoteSyncedTime(messageList.stream().mapToLong(SyncResponseMessage::getValue).max().orElse(-1));
  }
}
