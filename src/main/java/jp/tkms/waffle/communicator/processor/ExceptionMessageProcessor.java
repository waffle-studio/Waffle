package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.communicator.util.SelfCommunicativeEnvelope;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.response.ExceptionMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class ExceptionMessageProcessor extends ResponseProcessor<ExceptionMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<ExceptionMessage> messageList) throws ClassNotFoundException, IOException {
    for (ExceptionMessage message : messageList) {
      ErrorLogMessage.issue("Servant> " + message.getMessage());
    }
  }
}
