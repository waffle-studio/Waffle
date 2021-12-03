package jp.tkms.waffle.exception;

public class InvalidInputException extends WaffleException {
  public InvalidInputException(String input) {
    super(input);
  }
}
