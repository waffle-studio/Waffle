package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.PutTextFileMessage;
import jp.tkms.waffle.sub.servant.message.request.PutValueMessage;
import jp.tkms.waffle.sub.servant.message.response.JobCanceledMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PutTextFileRequestProcessor extends RequestProcessor<PutTextFileMessage> {
  static final int MAX_STREAM = 20;

  protected PutTextFileRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<PutTextFileMessage> messageList) throws ClassNotFoundException, IOException {
    ArrayList<BufferedOutputStream> streamList = new ArrayList<>();
    for (PutTextFileMessage message : messageList) {
      Path path = baseDirectory.resolve(message.getPath());
      BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path.toFile()));
      streamList.add(stream);
      stream.write(message.getValue().getBytes());

      if (streamList.size() > MAX_STREAM) {
        for (BufferedOutputStream s : streamList) {
          s.close();
        }
        streamList.clear();
      }
    }
    for (BufferedOutputStream s : streamList) {
      s.close();
    }
  }
}
