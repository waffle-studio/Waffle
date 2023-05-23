package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.message.response.RequestRepreparingMessage;

import java.io.IOException;
import java.util.ArrayList;

public class RequestRepreparingMessageProcessor extends ResponseProcessor<RequestRepreparingMessage> {
  @Override
  protected void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<RequestRepreparingMessage> messageList) throws ClassNotFoundException, IOException {
    for (RequestRepreparingMessage message : messageList) {
      AbstractTask job = submitter.findJobFromStore(message.getType(), message.getId());
      if (job != null) {
        try {
          job.setState(State.Retrying);
          job.setJobId("");
          InfoLogMessage.issue(job.getRun(), "will re-prepare");
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
        }
      }
    }
  }
}
