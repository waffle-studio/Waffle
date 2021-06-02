package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SubmitJobMessage extends JobMessage {
  String workingDirectory;
  String command;
  String xsubParameter;

  public SubmitJobMessage() {}

  public SubmitJobMessage(byte type, String id, Path workingDirectory, String command, String xsubParameter) {
    super(type, id);
    this.workingDirectory = workingDirectory.toString();
    this.command = command;
    this.xsubParameter = xsubParameter;
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
}
