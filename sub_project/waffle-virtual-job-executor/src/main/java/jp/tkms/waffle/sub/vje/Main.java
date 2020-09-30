package jp.tkms.waffle.sub.vje;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    boolean isLocalMode = true;
    int waitTime = 0;
    int shutdownTime = 0;
    int hesitationTime = 0;

    if (args.length < 4) {
      System.err.println("invalid arguments");
      System.err.println("<JAVA JAR> [MODE] [WAIT] [SHUTDOWN] [HESITATION]");
      System.exit(1);
    }

    if (args[0].toLowerCase().equals("mpi")) {
      isLocalMode = false;
    }

    try {
      waitTime = Integer.valueOf(args[1]);
      shutdownTime = Integer.valueOf(args[2]);
      hesitationTime = Integer.valueOf(args[3]);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.exit(1);
    }

    try {
      AbstractExecutor executor = null;
      if (isLocalMode) {
        executor = new LocalExecutor(waitTime, hesitationTime);
      } else {
        executor = new MPIExecutor(waitTime, hesitationTime);
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
