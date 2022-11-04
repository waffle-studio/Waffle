package jp.tkms.waffle.exception;

public class FailedToAcquireConnectionException extends RuntimeException {
  public FailedToAcquireConnectionException(Throwable e) {
    super(e);
  }
}
