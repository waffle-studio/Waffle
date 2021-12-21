package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.SubmitPodTaskMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.PodTaskRefusedMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import jp.tkms.waffle.sub.servant.pod.AbstractExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;

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
        Files.setPosixFilePermissions(workingDirectory, PosixFilePermissions.fromString("rwxrwx---"));
      } catch (IOException e) {
        //NOP
      }

      try {
        Path jobsDirectory = podPath.resolve(AbstractExecutor.JOBS_PATH);
        Path entitiesDirectory = podPath.resolve(AbstractExecutor.ENTITIES_PATH);

        Files.createDirectories(jobsDirectory);
        Files.createDirectories(entitiesDirectory);
        try {
          Files.setPosixFilePermissions(jobsDirectory, PosixFilePermissions.fromString("rwxrwx---"));
          Files.setPosixFilePermissions(entitiesDirectory, PosixFilePermissions.fromString("rwxrwx---"));
        } catch (IOException e) {
          //NOP
        }
        //Runtime.getRuntime().exec("chmod 777 '" + jobsDirectory.toString() + "'").waitFor();
        //Runtime.getRuntime().exec("chmod 777 '" + entitiesDirectory.toString() + "'").waitFor();

        if (!Files.exists(podPath.resolve(AbstractExecutor.HASH_IGNORE_FILE_PATH))) {
          Files.createFile(podPath.resolve(AbstractExecutor.HASH_IGNORE_FILE_PATH));
        }

        Files.writeString(entitiesDirectory.resolve(message.getId()), workingDirectory.toString());
        Files.createFile(jobsDirectory.resolve(message.getId()));
        try {
          Files.setPosixFilePermissions(entitiesDirectory.resolve(message.getId()), PosixFilePermissions.fromString("rw-rw----"));
          Files.setPosixFilePermissions(jobsDirectory.resolve(message.getId()), PosixFilePermissions.fromString("rw-rw----"));
        } catch (IOException e) {
          //NOP
        }
        //System.out.println(jsonObject.toString());
        response.add(new UpdateJobIdMessage(message, message.getJobId(), workingDirectory));
      } catch (Exception e) {
        e.printStackTrace();
        response.add(new JobExceptionMessage(message, e.getMessage()));
      }
    }
  }
}
