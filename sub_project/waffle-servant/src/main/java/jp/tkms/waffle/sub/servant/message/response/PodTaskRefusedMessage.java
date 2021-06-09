package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.JobMessage;

import java.nio.file.Path;

public class PodTaskRefusedMessage extends AbstractResponseMessage {
  String jobId;

  public PodTaskRefusedMessage() { }

  public PodTaskRefusedMessage(String jobId) {
    this.jobId = jobId;
  }

  public String getJobId() {
    return jobId;
  }
}
