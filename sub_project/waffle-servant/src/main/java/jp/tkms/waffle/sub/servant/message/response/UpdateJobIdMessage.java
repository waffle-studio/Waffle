package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;

public class UpdateJobIdMessage extends AbstractResponseMessage {
  byte type;
  String id;
  String jobId;

  public UpdateJobIdMessage() { }

  public UpdateJobIdMessage(byte type, String id, String jobId) {
    this.type = type;
    this.id = id;
    this.jobId = jobId;
  }

  public UpdateJobIdMessage(SubmitJobMessage submitJobMessage, String jobId) {
    this(submitJobMessage.getType(), submitJobMessage.getId(), jobId);
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
}
