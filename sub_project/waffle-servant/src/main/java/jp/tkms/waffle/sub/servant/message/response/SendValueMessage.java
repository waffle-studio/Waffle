package jp.tkms.waffle.sub.servant.message.response;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SendValueMessage extends AbstractResponseMessage {
  String workingDirectory;
  String id;
  String key;
  String operator;
  String value;

  public SendValueMessage() { }

  public SendValueMessage(Path workingDirectory, String id, String key, String operator, String value) {
    this.workingDirectory = workingDirectory.toString();
    this.id = id;
    this.key = key;
    this.operator = operator;
    this.value = value;
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }

  public String getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getFilterOperator() {
    return operator;
  }

  public String getFilterValue() {
    return value;
  }
}
