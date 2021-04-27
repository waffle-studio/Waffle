package jp.tkms.waffle.exception;

import java.nio.file.Path;

public class ChildProcedureNotFoundException extends WaffleException {
  public ChildProcedureNotFoundException(Throwable e) {
    super(e);
  }

  public ChildProcedureNotFoundException(Path path) {
    setMessage(path.toString() + " is not found");
  }
}
