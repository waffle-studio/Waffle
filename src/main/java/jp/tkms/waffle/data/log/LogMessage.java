package jp.tkms.waffle.data.log;

import jp.tkms.waffle.data.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogMessage extends Throwable {
  String message;

  public LogMessage(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void printMessage() {
    Log.create(this);
  }

  static String getStackTrace(Exception e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    printWriter.flush();
    return stringWriter.toString();
  }
}
