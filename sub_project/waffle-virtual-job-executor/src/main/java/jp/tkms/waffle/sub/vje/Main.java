package jp.tkms.waffle.sub.vje;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    /*
    if (args.length < 2) {
      System.err.println("invalid arguments");
      System.exit(1);
    }
     */
    try {
      AbstractExecutor executor = new LocalExecutor();
      executor.startPolling();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
