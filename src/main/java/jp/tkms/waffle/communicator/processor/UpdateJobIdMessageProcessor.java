package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;

import java.io.IOException;
import java.util.ArrayList;

public class UpdateJobIdMessageProcessor extends ResponseProcessor<UpdateJobIdMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<UpdateJobIdMessage> messageList) throws ClassNotFoundException, IOException {
    for (UpdateJobIdMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          job.setJobId(message.getJobId());
          //job.setState(State.Submitted);
          job.setState(State.Running);
          job.getRun().setRemoteWorkingDirectoryLog(message.getWorkingDirectory());
          InfoLogMessage.issue(job.getRun(), "was submitted");
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }
  }
}
