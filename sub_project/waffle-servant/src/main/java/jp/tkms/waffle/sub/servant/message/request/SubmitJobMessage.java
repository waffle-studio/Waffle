package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SubmitJobMessage extends AbstractRequestMessage {
  byte type;
  String id;
  String workingDirectory;
  String command;
  String xsubParameter;

  public SubmitJobMessage() {}

  public SubmitJobMessage(byte type, String id, Path workingDirectory, String command, String xsubParameter) {
    this.type = type;
    this.id = id;
    this.workingDirectory = workingDirectory.toString();
    this.command = command;
    this.xsubParameter = xsubParameter;
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }

  public String getCommand() {
    return command;
  }

  public String getXsubParameter() {
    return xsubParameter;
  }

  @Override
  void execute() {

  }
}
