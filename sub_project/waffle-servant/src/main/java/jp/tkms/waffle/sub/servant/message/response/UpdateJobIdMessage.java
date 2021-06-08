package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;

import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateJobIdMessage extends AbstractResponseMessage {
  byte type;
  String id;
  String jobId;
  String workingDirectory;

  public UpdateJobIdMessage() { }

  public UpdateJobIdMessage(byte type, String id, String jobId, Path workingDirectory) {
    this.type = type;
    this.id = id;
    this.jobId = jobId;
    this.workingDirectory = workingDirectory.toString();
  }

  public UpdateJobIdMessage(JobMessage submitJobMessage, String jobId, Path workingDirectory) {
    this(submitJobMessage.getType(), submitJobMessage.getId(), jobId, workingDirectory);
  }

  public byte getType() {
    return type;
  }

  public String getId() {
    return id;
  }

  public String getJobId() {
    return jobId;
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }
}
