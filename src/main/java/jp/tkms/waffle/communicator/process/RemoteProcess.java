package jp.tkms.waffle.communicator.process;

import java.io.InputStream;
import java.io.OutputStream;

public class RemoteProcess {

  private OutputStream outputStream;
  private InputStream inputStream;
  private InputStream errorStream;
  private Runnable finalizer = null;

  public void close() {
    if (finalizer != null) {
      finalizer.run();
    }
  }

  public void setStream(OutputStream outputStream, InputStream inputStream, InputStream errorStream) {
    setOutputStream(outputStream);
    setInputStream(inputStream);
    setErrorStream(errorStream);
  }

  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public void setErrorStream(InputStream errorStream) {
    this.errorStream = errorStream;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public InputStream getErrorStream() {
    return errorStream;
  }

  public void setFinalizer(Runnable runnable) {
    this.finalizer = runnable;
  }
}
