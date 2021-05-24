package jp.tkms.waffle.sub.servant;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import jp.tkms.waffle.sub.servant.message.AbstractMessage;
import org.apache.commons.io.IOUtils;

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
  }
}
