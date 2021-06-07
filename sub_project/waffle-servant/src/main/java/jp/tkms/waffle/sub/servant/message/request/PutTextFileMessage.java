package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PutTextFileMessage extends AbstractRequestMessage {
  String path;
  String value;

  public PutTextFileMessage() {}

  public PutTextFileMessage(Path path, String value) {
    this.path = path.toString();
    this.value = value;
  }

  public Path getPath() {
    return Paths.get(path);
  }

  public String getValue() {
    return value;
  }
}
