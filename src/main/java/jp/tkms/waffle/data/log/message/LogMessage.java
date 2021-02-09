package jp.tkms.waffle.data.log.message;

import jp.tkms.waffle.data.log.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class LogMessage extends Throwable {
  public static ExecutorService threadPool = Executors.newFixedThreadPool(16);

  String message;

  public LogMessage(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void printMessage() {
    threadPool.submit(() -> {
      Log.create(this);
    });
  }

  public static String getStackTrace(Throwable e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    printWriter.flush();
    return stringWriter.toString();
  }
}
