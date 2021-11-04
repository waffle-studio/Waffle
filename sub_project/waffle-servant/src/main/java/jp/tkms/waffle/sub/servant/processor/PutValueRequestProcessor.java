package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.GetValueCommand;
import jp.tkms.waffle.sub.servant.message.request.PutTextFileMessage;
import jp.tkms.waffle.sub.servant.message.request.PutValueMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class PutValueRequestProcessor extends RequestProcessor<PutValueMessage> {
  protected PutValueRequestProcessor() {
  }

  @Override
  protected void processIfMessagesExist(Path baseDirectory, Envelope request, Envelope response, ArrayList<PutValueMessage> messageList) throws ClassNotFoundException, IOException {
    for (PutValueMessage message : messageList) {
      try {
        GetValueCommand.putResponse(message);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
