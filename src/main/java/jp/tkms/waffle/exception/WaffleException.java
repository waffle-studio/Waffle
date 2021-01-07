package jp.tkms.waffle.exception;

public class WaffleException extends Exception {
  String message = "";

  public WaffleException() {
  }

  public WaffleException(Throwable e) {
    message = e.getMessage();
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
