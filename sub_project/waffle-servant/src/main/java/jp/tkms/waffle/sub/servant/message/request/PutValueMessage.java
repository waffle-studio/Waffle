package jp.tkms.waffle.sub.servant.message.request;

import jp.tkms.waffle.sub.servant.GetValueCommand;
import jp.tkms.waffle.sub.servant.message.response.SendValueMessage;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PutValueMessage extends AbstractRequestMessage {
  String workingDirectory;
  String id;
  String value;

  public PutValueMessage() {}

  public PutValueMessage(Path workingDirectory, String id, String value) {
    this.workingDirectory = workingDirectory.toString();
    this.id = id;
    this.value = value + GetValueCommand.RECORD_SEPARATING_MARK;
  }

  public PutValueMessage(SendValueMessage sendValueMessage, String value) {
    this(sendValueMessage.getWorkingDirectory(), sendValueMessage.getId(), value);
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }

  public String getId() {
    return id;
  }

  public String getValue() {
    return value;
  }
}
