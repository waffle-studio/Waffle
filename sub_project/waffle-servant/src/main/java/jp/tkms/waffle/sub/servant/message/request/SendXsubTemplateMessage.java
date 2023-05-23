package jp.tkms.waffle.sub.servant.message.request;

public class SendXsubTemplateMessage extends AbstractRequestMessage {

  String computerName;
  public SendXsubTemplateMessage() { }

  public SendXsubTemplateMessage(String computerName) {
    this.computerName = computerName;
  }

  public String getComputerName() {
    return computerName;
  }
}
