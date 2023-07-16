package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.UpdatePreparedMessage;

import java.io.IOException;
import java.util.ArrayList;

public class UpdatePreparedMessageProcessor extends ResponseProcessor<UpdatePreparedMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<UpdatePreparedMessage> messageList) throws ClassNotFoundException, IOException {
    for (UpdatePreparedMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          job.setState(State.Prepared);
          InfoLogMessage.issue(job.getRun(), "was prepared");
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }

    submitter.startupSubmittingProcessorManager();
  }
}
