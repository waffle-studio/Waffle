package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.*;
import jp.tkms.waffle.sub.servant.message.request.CollectPodTaskStatusMessage;
import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskFinishedMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateResultMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CollectPodTaskStatusRequestProcessor extends RequestProcessor<CollectPodTaskStatusMessage> {
  protected CollectPodTaskStatusRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CollectPodTaskStatusMessage> messageList) throws ClassNotFoundException, IOException {
    for (CollectPodTaskStatusMessage message : messageList) {
      try {
        new EventReader(baseDirectory, message.getWorkingDirectory().resolve(Constants.EVENT_FILE)).process((name, value) -> {
          response.add(new UpdateResultMessage(message, name, value));
        });

        PushFileCommand.process(message.getWorkingDirectory(), (m) -> response.add(m) );

        GetValueCommand.process(message.getWorkingDirectory(), (m) -> response.add(m) );

        Path jobsDirectory = baseDirectory.resolve(message.getPodDirectory()).resolve(AbstractExecutor.JOBS_PATH);
        if (message.isForceFinish() || !Files.exists(jobsDirectory.resolve(message.getId()))) {
          int exitStatus = -2;
          try {
            exitStatus = Integer.parseInt(new String(Files.readAllBytes(baseDirectory.resolve(message.getWorkingDirectory()).resolve(Constants.EXIT_STATUS_FILE))));
          } catch (Exception | Error e) {
            exitStatus = -1;
          }
          if ((exitStatus >= 0 && new DirectoryHash(baseDirectory, message.getWorkingDirectory()).isMatchToHashFile()) || exitStatus < 0) {
            //(new DirectoryHash(baseDirectory, message.getWorkingDirectory())).waitToMatch(Constants.DIRECTORY_SYNCHRONIZATION_TIMEOUT);
            response.add(new UpdateStatusMessage(message, exitStatus));
            response.add(message.getWorkingDirectory().resolve(Constants.STDOUT_FILE));
            response.add(message.getWorkingDirectory().resolve(Constants.STDERR_FILE));
            response.add(new PodTaskFinishedMessage(message));
          } else {
            response.add(new UpdateStatusMessage(message));
          }
        } else {
          response.add(new UpdateStatusMessage(message));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
