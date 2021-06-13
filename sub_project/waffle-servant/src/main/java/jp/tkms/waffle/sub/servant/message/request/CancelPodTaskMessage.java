package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CancelPodTaskMessage extends JobMessage {
  String podDirectory;
  String workingDirectory;

  public CancelPodTaskMessage() { }

  public CancelPodTaskMessage(byte type, String id, Path podDirectory, Path workingDirectory) {
    super(type, id);
    this.podDirectory = podDirectory.toString();
    this.workingDirectory = workingDirectory.toString();
  }

  public Path getPodDirectory() {
    return Paths.get(podDirectory);
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }
}
