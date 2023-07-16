package jp.tkms.waffle.sub.servant.processor;

import jp.tkms.waffle.sub.servant.Constants;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.PutTextFileMessage;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class PutTextFileRequestProcessor extends RequestProcessor<PutTextFileMessage> {

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

      if (streamList.size() > Constants.MAX_STREAM) {
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
