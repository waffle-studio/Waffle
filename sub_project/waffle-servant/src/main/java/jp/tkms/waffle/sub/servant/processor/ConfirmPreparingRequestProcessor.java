package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.ConfirmPreparingMessage;
import jp.tkms.waffle.sub.servant.message.response.RequestRepreparingMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdatePreparedMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ConfirmPreparingRequestProcessor extends RequestProcessor<ConfirmPreparingMessage> {

  protected ConfirmPreparingRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<ConfirmPreparingMessage> messageList) throws ClassNotFoundException, IOException {
    Set<String> ignoreSet = new HashSet<>();
    for (RequestRepreparingMessage message : response.getMessageBundle().getCastedMessageList(RequestRepreparingMessage.class)) {
      ignoreSet.add(message.getId());
    }

    Set<String> addedSet = new HashSet<>();
    for (ConfirmPreparingMessage message : messageList) {
      if (addedSet.add(message.getId()) && !ignoreSet.contains(message.getId())) {
        response.add(new UpdatePreparedMessage(message));
      }
    }
  }
}
