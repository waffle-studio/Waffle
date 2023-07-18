package jp.tkms.waffle.sub.servant.message.response;

import jp.tkms.waffle.sub.servant.message.request.SendXsubTemplateMessage;

public class UpdateXsubTemplateMessage extends AbstractResponseMessage {
  String computerName;
  String template;
  String options;

  public UpdateXsubTemplateMessage() { }

  public UpdateXsubTemplateMessage(SendXsubTemplateMessage message, String template, String options) {
    this.computerName = message.getComputerName();
    this.template = template;
    this.options = options;
  }

  public String getComputerName() {
    return computerName;
  }

  public String getTemplate() {
    return template;
  }

  public String getOptions() {
    return options;
  }
}
