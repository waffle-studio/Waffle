package jp.tkms.waffle.sub.vje;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    boolean isLocalMode = true;
    int shutdownTime = 0;
    int waitTime = 0;

    if (args.length < 3) {
      System.err.println("invalid arguments");
      System.err.println("<JAVA JAR> [MODE] [WAIT] [SHUTDOWN]");
      System.exit(1);
    }

    if (args[0].toLowerCase().equals("mpi")) {
      isLocalMode = false;
    }

    try {
      waitTime = Integer.valueOf(args[1]);
      shutdownTime = Integer.valueOf(args[2]);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.exit(1);
    }

    try {
      AbstractExecutor executor = null;
      if (isLocalMode) {
        executor = new LocalExecutor();
      } else {
        executor = new MPIExecutor();
      }

      final int finalShutdownTime = shutdownTime;
      final AbstractExecutor finalExecutor = executor;
      Thread shutdownTimer = new Thread() {
        @Override
        public void run() {
          for (int countDown = finalShutdownTime; countDown >= 0; countDown--) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          finalExecutor.shutdown();
        }
      };
      shutdownTimer.start();

      executor.startPolling();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return;
  }
}
