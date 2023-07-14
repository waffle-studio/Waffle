package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.CheckJobIdMessage;
import jp.tkms.waffle.sub.servant.message.response.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CheckJobIdRequestProcessor extends RequestProcessor<CheckJobIdMessage> {
  protected CheckJobIdRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CheckJobIdMessage> messageList) throws ClassNotFoundException, IOException {
    messageList.stream().parallel().forEach(message-> {
      Path workingDirectory = baseDirectory.resolve(message.getWorkingDirectory()).toAbsolutePath().normalize();
      String jobId = "";
      if (Files.exists(workingDirectory)) {
        Path jobIdPath = workingDirectory.resolve(Constants.JOBID_FILE);
        if (Files.exists(jobIdPath)) {
          try {
            jobId = new String(Files.readAllBytes(jobIdPath));
          } catch (IOException e) {
            response.add(new JobExceptionMessage(message, e.toString()));
          }
        }
      }

      jobId = jobId.trim();
      if ("".equals(jobId)) {
        response.add(new RequestRepreparingMessage(message));
      } else {
        response.add(new UpdateJobIdMessage(message, jobId, workingDirectory));
      }
    });
  }
}
