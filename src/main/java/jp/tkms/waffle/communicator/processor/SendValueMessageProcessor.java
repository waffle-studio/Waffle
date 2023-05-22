package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.IndirectValue;
import jp.tkms.waffle.manager.Filter;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.PutValueMessage;
import jp.tkms.waffle.sub.servant.message.response.SendValueMessage;

import java.io.IOException;
import java.util.ArrayList;

public class SendValueMessageProcessor extends ResponseProcessor<SendValueMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<SendValueMessage> messageList) throws ClassNotFoundException, IOException {
    Envelope replies = submitter.createEnvelope();
    for (SendValueMessage message : messageList) {
      String value = null;
      try {
        value = IndirectValue.convert(message.getKey()).getString();
      } catch (WarnLogMessage e) {
        value = null;
      }
      if (message.getFilterOperator().equals("")
        || (new Filter(message.getFilterOperator(), message.getFilterValue())).apply(value)
      ) {
        replies.add(new PutValueMessage(message, value == null ? "null" : value));
      }
    }
    if (!replies.isEmpty()) {
      sendAndReceiveEnvelope(replies);
    }
  }
}
