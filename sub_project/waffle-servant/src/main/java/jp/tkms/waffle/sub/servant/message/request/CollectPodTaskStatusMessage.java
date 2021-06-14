package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CollectPodTaskStatusMessage extends JobMessage {
  boolean forceFinish;
  String podDirectory;
  String workingDirectory;

  public CollectPodTaskStatusMessage() { }

  public CollectPodTaskStatusMessage(byte type, String id, boolean forceFinish, Path podDirectory, Path workingDirectory) {
    super(type, id);
    this.forceFinish = forceFinish;
    this.podDirectory = podDirectory.toString();
    this.workingDirectory = workingDirectory.toString();
  }

  public boolean isForceFinish() {
    return forceFinish;
  }

  public Path getPodDirectory() {
    return Paths.get(podDirectory);
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }
}
