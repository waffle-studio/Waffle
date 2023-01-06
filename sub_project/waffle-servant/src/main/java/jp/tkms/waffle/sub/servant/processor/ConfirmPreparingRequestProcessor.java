package jp.tkms.waffle.sub.servant.processor;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.sub.servant.DirectoryHash;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.XsubFile;
import jp.tkms.waffle.sub.servant.message.request.ConfirmPreparingMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.message.response.*;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;

public class ConfirmPreparingRequestProcessor extends RequestProcessor<ConfirmPreparingMessage> {

  protected ConfirmPreparingRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<ConfirmPreparingMessage> messageList) throws ClassNotFoundException, IOException {
    Set<String> ignoreSet = new HashSet<>();
    for (RequestRepreparingMessage message : response.getMessageBundle().getCastedMessageList(RequestRepreparingMessage.class)) {
      ignoreSet.add(message.getId());
    }

    Set<String> addedSet = new HashSet<>();
    for (ConfirmPreparingMessage message : messageList) {
      if (addedSet.add(message.getId()) && !ignoreSet.contains(message.getId())) {
        response.add(new UpdatePreparedMessage(message));
      }
    }
  }
}
