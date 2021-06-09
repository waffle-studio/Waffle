package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitPodTaskMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskRefusedMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
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
import java.util.Map;

public class SubmitPodTaskRequestProcessor extends RequestProcessor<SubmitPodTaskMessage> {
  protected SubmitPodTaskRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<SubmitPodTaskMessage> messageList) throws ClassNotFoundException, IOException {
    for (SubmitPodTaskMessage message : messageList) {
      Path podPath = baseDirectory.resolve(message.getPodDirectory());

      if (!Files.exists(podPath.resolve(AbstractExecutor.UPDATE_FILE_PATH)) || Files.exists(podPath.resolve(AbstractExecutor.LOCKOUT_FILE_PATH))) {
        response.add(new PodTaskRefusedMessage(message.getJobId()));
        continue;
      }

      if (message.getExecutableDirectory() != null) {
        DirectoryHash executableDirectoryHash = new DirectoryHash(baseDirectory, message.getExecutableDirectory(), false);
        if (!executableDirectoryHash.hasHashFile()) {
          executableDirectoryHash.save();
        } else {
          if (executableDirectoryHash.update()) {
            System.out.println("!!!!! EXECUTABLE FILES HAS CHANGED !!!!!");
            //TODO: notify if hash changed
          }
        }
      }

      Path workingDirectory = baseDirectory.resolve(message.getWorkingDirectory()).toAbsolutePath().normalize();
      new DirectoryHash(baseDirectory, workingDirectory).save();

      try {
        Path jobsDirectory = podPath.resolve(AbstractExecutor.JOBS_PATH);
        Path entitiesDirectory = podPath.resolve(AbstractExecutor.ENTITIES_PATH);

        Files.createDirectories(jobsDirectory);
        Runtime.getRuntime().exec("chmod 777 '" + jobsDirectory.toString() + "'");
        Files.createDirectories(entitiesDirectory);
        Runtime.getRuntime().exec("chmod 777 '" + entitiesDirectory.toString() + "'");

        if (!Files.exists(podPath.resolve(AbstractExecutor.HASH_IGNORE_FILE_PATH))) {
          Files.createFile(podPath.resolve(AbstractExecutor.HASH_IGNORE_FILE_PATH));
        }

        Files.writeString(entitiesDirectory.resolve(message.getId()), workingDirectory.toString());
        Runtime.getRuntime().exec("chmod 666 '" + entitiesDirectory.resolve(message.getId()) + "'");
        Files.createFile(jobsDirectory.resolve(message.getId()));
        Runtime.getRuntime().exec("chmod 666 '" + jobsDirectory.resolve(message.getId()) + "'");
        //System.out.println(jsonObject.toString());
        response.add(new UpdateJobIdMessage(message, message.getJobId(), workingDirectory));
      } catch (Exception e) {
        //e.printStackTrace();
        response.add(new JobExceptionMessage(message, e.getMessage()));
      }
    }
  }
}
