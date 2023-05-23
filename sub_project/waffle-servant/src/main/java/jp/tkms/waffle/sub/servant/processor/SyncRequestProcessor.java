package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.CancelPodTaskMessage;
import jp.tkms.waffle.sub.servant.message.request.SyncRequestMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskCanceledMessage;
import jp.tkms.waffle.sub.servant.message.response.SyncResponseMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class SyncRequestProcessor extends RequestProcessor<SyncRequestMessage> {

  protected SyncRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<SyncRequestMessage> messageList) throws ClassNotFoundException, IOException {
    messageList.stream().forEach(message -> {
      response.add(new SyncResponseMessage(message));
    });
  }
}
