package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CollectStatusMessage extends JobMessage {
  String jobId;
  String workingDirectory;

  public CollectStatusMessage() { }

  public CollectStatusMessage(byte type, String id, String jobId, Path workingDirectory) {
    super(type, id);
    this.jobId = jobId;
    this.workingDirectory = workingDirectory.toString();
  }

  public String getJobId() {
    return jobId;
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }
}
