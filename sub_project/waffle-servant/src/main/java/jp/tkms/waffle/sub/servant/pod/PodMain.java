package jp.tkms.waffle.sub.servant.pod;

import java.io.IOException;

public class PodMain {
    public static Thread shutdownTimer = null;
    public static void main(String[] args) {
    boolean isLocalMode = true;
    int timeout = 0;
    int shutdownTime = 0;
    int marginTime = 0;

    if (args.length != 4) {
      System.err.println("invalid arguments");
      System.err.println("<JAVA JAR> [MODE] [TIMEOUT] [FORCE SHUTDOWN TIME] [SHUTDOWN PREPARATION MARGIN]");
      System.err.println("invalid arguments");
      System.exit(1);
    }

    if (args[0].toLowerCase().equals("mpi")) {
      isLocalMode = false;
    }

    try {
      timeout = Integer.valueOf(args[1]);
      shutdownTime = Integer.valueOf(args[2]);
      marginTime = Integer.valueOf(args[3]);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.exit(1);
    }

    if (shutdownTime < marginTime) {
      System.err.println("SHUTDOWN PREPARATION MARGIN must be less than FORCE SHUTDOWN TIME");
      System.exit(1);
    }

    try {
      AbstractExecutor executor = null;
      if (isLocalMode) {
        executor = new LocalExecutor(timeout, marginTime);
      } else {
        executor = new MPIExecutor(timeout, marginTime);
      }

      final int finalShutdownTime = shutdownTime - marginTime;
      final AbstractExecutor finalExecutor = executor;
      shutdownTimer = new Thread() {
        @Override
        public void run() {
          for (int countDown = finalShutdownTime; countDown >= 0; countDown--) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              return;
            }
          }

          finalExecutor.shutdown();
        }
      };
      shutdownTimer.start();

      executor.startPolling(shutdownTimer);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return;
  }
}
