package jp.tkms.waffle.sub.servant.message.response;

public class XsubTemplateMessage extends AbstractResponseMessage {
  String template;

  public XsubTemplateMessage() { }

  public XsubTemplateMessage(String template) {
    this.template = template;
  }

  public String getTemplate() {
    return template;
  }
}
