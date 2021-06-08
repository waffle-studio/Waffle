package jp.tkms.waffle.sub.servant.message.request;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SubmitPodTaskMessage extends JobMessage {
  String jobId;
  String podDirectory;
  String workingDirectory;
  String executableDirectory;

  public SubmitPodTaskMessage() {}

  public SubmitPodTaskMessage(byte type, String id, String jobId, Path podDirectory, Path workingDirectory, Path executableDirectory) {
    super(type, id);
    this.jobId = jobId;
    this.podDirectory = podDirectory.toString();
    this.workingDirectory = workingDirectory.toString();
    this.executableDirectory = executableDirectory == null ? null : executableDirectory.toString();
  }

  public String getJobId() {
    return jobId;
  }

  public Path getPodDirectory() {
    return Paths.get(podDirectory);
  }

  public Path getWorkingDirectory() {
    return Paths.get(workingDirectory);
  }

  public Path getExecutableDirectory() {
    return executableDirectory == null ? null : Paths.get(executableDirectory);
  }
}
