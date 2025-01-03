package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.CollectPodStatusMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdatePodStatusMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CollectPodStatusRequestProcessor extends RequestProcessor<CollectPodStatusMessage> {
  protected CollectPodStatusRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CollectPodStatusMessage> messageList) throws ClassNotFoundException, IOException {
    messageList.stream().parallel().forEach(message -> {
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
    });
  }
}
