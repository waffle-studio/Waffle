package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.sub.servant.message.response.StorageWarningMessage;

import java.io.IOException;
import java.util.ArrayList;

public class StorageWarningMessageProcessor extends ResponseProcessor<StorageWarningMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<StorageWarningMessage> messageList) throws ClassNotFoundException, IOException {
    for (StorageWarningMessage message : messageList) {
      ErrorLogMessage.issue(submitter.getComputer(), message.getMessage() + " (Remaining Byte & INode)");
    }
  }
}
