package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SubmitJobMessage extends JobMessage {
  String workingDirectory;
  String executableDirectory;
  String command;
  String xsubParameter;

  public SubmitJobMessage() {}

  public SubmitJobMessage(byte type, String id, Path workingDirectory, Path executableDirectory, String command, String xsubParameter) {
    super(type, id);
    this.workingDirectory = workingDirectory.toString();
    this.executableDirectory = executableDirectory == null ? null : executableDirectory.toString();
    this.command = command;
    this.xsubParameter = xsubParameter;
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }

  public Path getExecutableDirectory() {
    return executableDirectory == null ? null : Paths.get(executableDirectory);
  }

  public String getCommand() {
    return command;
  }

  public String getXsubParameter() {
    return xsubParameter;
  }
}
