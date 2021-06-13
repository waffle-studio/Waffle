package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.EventReader;
import jp.tkms.waffle.sub.servant.message.request.CancelPodTaskMessage;
import jp.tkms.waffle.sub.servant.message.request.CollectPodTaskStatusMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskCanceledMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskFinishedMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateResultMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CancelPodTaskRequestProcessor extends RequestProcessor<CancelPodTaskMessage> {

  protected CancelPodTaskRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CancelPodTaskMessage> messageList) throws ClassNotFoundException, IOException {
    for (CancelPodTaskMessage message : messageList) {
      try {
        Path jobFlag = baseDirectory.resolve(message.getPodDirectory()).resolve(AbstractExecutor.JOBS_PATH).resolve(message.getId());
        Files.deleteIfExists(jobFlag);
        response.add(new PodTaskCanceledMessage(message));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
