package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CollectPodStatusMessage extends AbstractRequestMessage {
  String id;
  String directory;

  public CollectPodStatusMessage() {
  }

  public CollectPodStatusMessage(String podId, Path podDirectory) {
    this.id = podId;
    this.directory = podDirectory.toString();
  }

  public String getId() {
    return id;
  }

  public Path getDirectory() {
    return Paths.get(directory);
  }
}
