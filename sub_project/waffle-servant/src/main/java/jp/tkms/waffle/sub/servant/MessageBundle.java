package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.Json;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import jp.tkms.waffle.sub.servant.message.AbstractMessage;
import jp.tkms.waffle.sub.servant.message.request.CollectStatusMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.JobExceptionMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateJobIdMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateResultMessage;
import jp.tkms.waffle.sub.servant.message.response.UpdateStatusMessage;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MessageBundle {
  private HashMap<Class<? extends AbstractMessage>, ArrayList<AbstractMessage>> messageListSet;

  MessageBundle() {
    // for serialization
    messageListSet =  new HashMap<>();
  }

  public ArrayList<AbstractMessage> getMessageList(Class<? extends AbstractMessage> messageClass) {
    ArrayList<AbstractMessage> messageList = messageListSet.get(messageClass);
    if (messageList == null) {
      messageList = new ArrayList<>();
      messageListSet.put(messageClass, messageList);
    }
    return messageList;
  }

  public <T extends AbstractMessage> ArrayList<T> getCastedMessageList(Class<T> messageClass) {
    ArrayList<T> castedMessageList = new ArrayList<>();
    for (AbstractMessage message : getMessageList(messageClass)) {
      castedMessageList.add((T)message);
    }
    return castedMessageList;
  }

  /*
  public void print(String tag) {
    System.out.println(tag + "{");
    for (Map.Entry<Class<? extends AbstractMessage>, ArrayList<AbstractMessage>> entry : messageListSet.entrySet()) {
      System.out.println(entry.getKey().getName() + " : " + entry.getValue().size());
    }
    System.out.println("}");
  }
   */

  public void add(AbstractMessage message) {
    getMessageList(message.getClass()).add(message);
  }

  public void serialize(OutputStream stream) throws IOException {
    //GZIPOutputStream outputStream = new GZIPOutputStream(stream);;
    Kryo kryo = new Kryo();
    registerClassesToKryo(kryo);
    Output output = new Output(stream);
    kryo.writeObject(output, this);
    output.flush();
  }

  public static MessageBundle load(InputStream stream) throws IOException {
    //GZIPInputStream inputStream = new GZIPInputStream(stream);
    Kryo kryo = new Kryo();
    registerClassesToKryo(kryo);
    Input input = new Input(stream);
    MessageBundle data = kryo.readObject(input, MessageBundle.class);
    return data;
  }

  public static void registerClassesToKryo(Kryo kryo) {
    kryo.register(MessageBundle.class);
    kryo.register(java.util.HashMap.class);
    kryo.register(Class.class);
    kryo.register(ArrayList.class);
    kryo.register(SubmitJobMessage.class);
    kryo.register(JobExceptionMessage.class);
    kryo.register(UpdateJobIdMessage.class);
    kryo.register(CollectStatusMessage.class);
    kryo.register(UpdateStatusMessage.class);
    kryo.register(UpdateResultMessage.class);
  }
}
