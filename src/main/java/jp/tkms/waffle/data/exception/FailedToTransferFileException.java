package jp.tkms.waffle.data.exception;

import java.io.IOException;

public class FailedToTransferFileException extends WaffleException {
  public FailedToTransferFileException(Throwable e) {
    super(e);
  }

  public FailedToTransferFileException(IOException e) {
    setMessage("could not transfer '" + e.getMessage() + "'");
  }
}
