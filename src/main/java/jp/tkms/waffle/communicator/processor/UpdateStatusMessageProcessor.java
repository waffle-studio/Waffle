package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;

import java.io.IOException;
import java.util.ArrayList;

public class UpdateStatusMessageProcessor extends ResponseProcessor<UpdateStatusMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<UpdateStatusMessage> messageList) throws ClassNotFoundException, IOException {
    for (UpdateStatusMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          if (message.isFinished()) {
            job.getRun().setExitStatus(message.getExitStatus());
            job.setState(State.Finalizing);
            submitter.startupFinishedProcessorManager();
            submitter.startupSubmittingProcessorManager();
          }
          /*
          else {
            if (job.getState().equals(State.Submitted)) {
              job.setState(State.Running);
            }
          }
           */
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }
  }
}
