package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.CancelJobMessage;
import jp.tkms.waffle.sub.servant.message.request.PutTextFileMessage;
import jp.tkms.waffle.sub.servant.message.response.JobCanceledMessage;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PutTextFileRequestProcessor extends RequestProcessor<PutTextFileMessage> {
  protected PutTextFileRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<PutTextFileMessage> messageList) throws ClassNotFoundException, IOException {
    messageList.stream().parallel().forEach(message -> {
      try {
        Path path = baseDirectory.resolve(message.getPath());
        Files.createDirectories(path.getParent());
        Files.writeString(path, message.getValue(), Charset.forName("UTF-8"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
