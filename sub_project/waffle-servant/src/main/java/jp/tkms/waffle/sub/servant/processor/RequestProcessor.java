package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.AbstractRequestMessage;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class RequestProcessor<T extends AbstractRequestMessage> {
  public static void processMessages(Path baseDirectory, Envelope request, Envelope response) throws ClassNotFoundException, IOException {
    for (RequestProcessor processor : new RequestProcessor[]{
      new ConfirmPreparingRequestProcessor(),
      new ChangePermssionRequestProcessor(),
      new PutTextFileRequestProcessor(),
      new PutValueRequestProcessor(),
      new SendXsubTemplateRequestProcessor(),
      new CancelJobRequestProcessor(),
      new CancelPodTaskRequestProcessor(),
      new SubmitJobRequestProcessor(),
      new SubmitPodTaskRequestProcessor(),
      new CollectStatusRequestProcessor(),
      new CollectPodTaskStatusRequestProcessor(),
      new CollectPodStatusRequestProcessor()
    }) {
      processor.processIfMessagesExist(baseDirectory, request, response);
    }
  }

  protected RequestProcessor() {
  }

  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response) throws ClassNotFoundException, IOException {
    ArrayList<T> messageList = getMessageList(request);
    if (!messageList.isEmpty()) {
      processIfMessagesExist(baseDirectory, request, response, messageList);
    }
  }

  protected abstract void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<T> messageList) throws ClassNotFoundException, IOException;

  private ArrayList<T> getMessageList(Envelope request) throws ClassNotFoundException {
    Class messageClass = Class.forName(((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName());
    return request.getMessageBundle().getCastedMessageList(messageClass);
  }
}
