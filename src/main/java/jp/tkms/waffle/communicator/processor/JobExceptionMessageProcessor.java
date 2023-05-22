package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;

import java.io.IOException;
import java.util.ArrayList;

public class JobExceptionMessageProcessor extends ResponseProcessor<JobExceptionMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<JobExceptionMessage> messageList) throws ClassNotFoundException, IOException {
    for (JobExceptionMessage message : messageList) {
      WarnLogMessage.issue("Servant:JobException> " + message.getMessage());

      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }
  }
}
