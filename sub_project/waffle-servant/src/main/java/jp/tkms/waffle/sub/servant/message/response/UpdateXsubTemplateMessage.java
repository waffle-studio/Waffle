package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.SendXsubTemplateMessage;

public class UpdateXsubTemplateMessage extends AbstractResponseMessage {
  String computerName;
  String template;

  public UpdateXsubTemplateMessage() { }

  public UpdateXsubTemplateMessage(SendXsubTemplateMessage message, String template) {
    this.computerName = message.getComputerName();
    this.template = template;
  }

  public String getComputerName() {
    return computerName;
  }

  public String getTemplate() {
    return template;
  }
}
