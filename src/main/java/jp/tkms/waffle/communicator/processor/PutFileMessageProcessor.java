package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.sub.servant.message.response.PutFileMessage;

import java.io.IOException;
import java.util.ArrayList;

public class PutFileMessageProcessor extends ResponseProcessor<PutFileMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<PutFileMessage> messageList) throws ClassNotFoundException, IOException {
    for (PutFileMessage message : messageList) {
      message.putFile();
    }
  }
}
