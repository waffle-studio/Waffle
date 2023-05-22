package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.JobCanceledMessage;

import java.io.IOException;
import java.util.ArrayList;

public class JobCanceledMessageProcessor extends ResponseProcessor<JobCanceledMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<JobCanceledMessage> messageList) throws ClassNotFoundException, IOException {
    for (JobCanceledMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          if (job.getState().equals(State.Abort)) {
            job.setState(State.Aborted);
          }
          if (job.getState().equals(State.Cancel)) {
            job.setState(State.Canceled);
          }
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }
  }
}
