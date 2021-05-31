package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.AbstractRequestMessage;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class RequestProcessor<T extends AbstractRequestMessage> {
  public static void processMessages(Path baseDirectory, Envelope request, Envelope response) throws ClassNotFoundException, IOException {
    new SubmitJobRequestProcessor(request, response).process(baseDirectory);
  }

  Envelope request;
  Envelope response;

  protected RequestProcessor(Envelope request, Envelope response) {
    this.request = request;
    this.response = response;
  }

  protected abstract void process(Path baseDirectory) throws ClassNotFoundException, IOException;

  protected ArrayList<T> getMessageList() throws ClassNotFoundException {
    Class messageClass = Class.forName(((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName());
    return request.getMessageBundle().getCastedMessageList(messageClass);
  }
}
