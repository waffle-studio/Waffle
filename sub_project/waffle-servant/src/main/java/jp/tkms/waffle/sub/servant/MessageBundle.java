package jp.tkms.waffle.sub.servant;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import jp.tkms.waffle.sub.servant.message.AbstractMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageBundle {
  private HashMap<Class<? extends AbstractMessage>, ArrayList<AbstractMessage>> messageListSet =  new HashMap<>();

  MessageBundle() {
    // for serialization
  }

  public ArrayList<AbstractMessage> getMessageList(Class<? extends AbstractMessage> messageClass) {
    ArrayList<AbstractMessage> messageList = messageListSet.get(messageClass);
    if (messageList == null) {
      messageList = new ArrayList<>();
      messageListSet.put(messageClass, messageList);
    }
    return messageList;
  }

  public void add(AbstractMessage message) {
    getMessageList(message.getClass()).add(message);
  }

  public void serialize(OutputStream stream) throws IOException {
    GZIPOutputStream outputStream = new GZIPOutputStream(stream);;
    Kryo kryo = new Kryo();
    Output output = new Output(outputStream);
    kryo.writeObject(output, this);
    output.close();
    outputStream.close();
  }

  public static MessageBundle load(InputStream stream) throws IOException {
    GZIPInputStream inputStream = new GZIPInputStream(stream);
    Kryo kryo = new Kryo();
    Input input = new Input(inputStream);
    MessageBundle data = kryo.readObject(input, MessageBundle.class);
    input.close();
    return data;
  }
}
