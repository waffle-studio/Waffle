package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.CheckJobIdMessage;
import jp.tkms.waffle.sub.servant.message.response.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class CheckJobIdRequestProcessor extends RequestProcessor<CheckJobIdMessage> {
  public static final int MAX_REMOVING_RETRYING = 100;
  protected CheckJobIdRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<CheckJobIdMessage> messageList) throws ClassNotFoundException, IOException {
    ArrayList<Path> removingList = new ArrayList<>();

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
        removingList.add(workingDirectory);
        response.add(new RequestRepreparingMessage(message));
      } else {
        response.add(new UpdateJobIdMessage(message, jobId, workingDirectory));
      }
    });

    if (!removingList.isEmpty()) {
      try {
        TimeUnit.SECONDS.sleep(Constants.WATCHDOG_INTERVAL);
      } catch (InterruptedException e) {
        //NOP
      }
      removingList.stream().parallel().forEach(path -> {
        if (Files.exists(path)) {
          for (int count = 0; count < MAX_REMOVING_RETRYING; count += 1) {
            try {
              Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
              continue;
            }
            break;
          }
        }
      });
    }
  }
}
