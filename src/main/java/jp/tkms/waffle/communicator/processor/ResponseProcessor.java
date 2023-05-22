package jp.tkms.waffle.communicator.processor;

import jp.tkms.waffle.communicator.AbstractSubmitter;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.response.*;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

public abstract class ResponseProcessor<T extends AbstractResponseMessage> {
  private static final ResponseProcessor[] processors = {
    new ExceptionMessageProcessor(),
    new UpdateXsubTemplateMessageProcessor(),
    new StorageWarningMessageProcessor(),
    new JobExceptionMessageProcessor(),
    new RequestRepreparingMessageProcessor(),
    new UpdatePreparedMessageProcessor(),
    new PutFileMessageProcessor(),
    new UpdateResultMessageProcessor(),
    new JobCanceledMessageProcessor(),
    new UpdateJobIdMessageProcessor(),
    new UpdateStatusMessageProcessor(),
    new SendValueMessageProcessor()
  };

  public static void processMessages(AbstractSubmitter submitter, Envelope response) throws ClassNotFoundException, IOException {
    for (ResponseProcessor processor : processors) {
      processor.processIfMessagesExist(submitter, response);
    }
  }

  protected ResponseProcessor() {
  }

  protected void processIfMessagesExist(AbstractSubmitter submitter, Envelope response) throws ClassNotFoundException, IOException {
    ArrayList<T> messageList = getMessageList(response);
    if (!messageList.isEmpty()) {
      processIfMessagesExist(submitter, messageList);
    }
  }

  protected abstract void processIfMessagesExist(AbstractSubmitter submitter, ArrayList<T> messageList) throws ClassNotFoundException, IOException;

  private ArrayList<T> getMessageList(Envelope response) throws ClassNotFoundException {
    Class messageClass = Class.forName(((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName());
    return response.getMessageBundle().getCastedMessageList(messageClass);
  }
}
