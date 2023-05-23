package jp.tkms.waffle.communicator.processor;

import jp.tkms.utils.concurrent.LockByKey;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.UpdateResultMessage;

import java.io.IOException;
import java.util.ArrayList;

public class UpdateResultMessageProcessor extends ResponseProcessor<UpdateResultMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<UpdateResultMessage> messageList) throws ClassNotFoundException, IOException {
    for (UpdateResultMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        if (job instanceof ExecutableRunTask) {
          try {
            ExecutableRun run = ((ExecutableRunTask) job).getRun();
            Object value = message.getValue();
            try (LockByKey lock = LockByKey.acquire(job.getHexCode())) {
              if (message.getValue().indexOf('.') < 0) {
                value = Integer.valueOf(message.getValue());
              } else {
                value = Double.valueOf(message.getValue());
              }
            } catch (Exception e) {
              if (message.getValue().equalsIgnoreCase("true")) {
                value = Boolean.TRUE;
              } else if (message.getValue().equalsIgnoreCase("false")) {
                value = Boolean.FALSE;
              }
            }
            run.putResultDynamically(message.getKey(), value);
          } catch (RunNotFoundException e) {
            WarnLogMessage.issue(e);
          }
        }
      }
    }
  }
}
