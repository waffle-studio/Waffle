package jp.tkms.waffle.sub.servant.message.request;

public class CancelJobMessage extends JobMessage {
  String jobId;

  public CancelJobMessage() {}

  public CancelJobMessage(byte type, String id, String jobId) {
    super(type, id);
    this.jobId = jobId;
  }

  public String getJobId() {
    return jobId;
  }
}
