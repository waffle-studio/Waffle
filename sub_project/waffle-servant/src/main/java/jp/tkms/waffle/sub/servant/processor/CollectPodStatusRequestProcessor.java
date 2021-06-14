package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.EventReader;
import jp.tkms.waffle.sub.servant.message.request.CollectPodStatusMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskFinishedMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdatePodStatusMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateResultMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public class CollectPodStatusRequestProcessor extends RequestProcessor<CollectPodStatusMessage> {
  protected CollectPodStatusRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CollectPodStatusMessage> messageList) throws ClassNotFoundException, IOException {
    for (CollectPodStatusMessage message : messageList) {
      if (Files.exists(baseDirectory.resolve(message.getDirectory()).resolve(AbstractExecutor.LOCKOUT_FILE_PATH))) {
        if (AbstractExecutor.isAlive(baseDirectory.resolve(message.getDirectory()))) {
          response.add(new UpdatePodStatusMessage(message.getId(), UpdatePodStatusMessage.LOCKED));
        } else {
          AbstractExecutor.removeAllJob(baseDirectory.resolve(message.getDirectory()));
          response.add(new UpdatePodStatusMessage(message.getId(), UpdatePodStatusMessage.FINISHED));
        }
      } else {
        if (Files.exists(baseDirectory.resolve(message.getDirectory()).resolve(AbstractExecutor.UPDATE_FILE_PATH))) {
          response.add(new UpdatePodStatusMessage(message.getId(), UpdatePodStatusMessage.RUNNING));
        }
      }
    }
  }
}
