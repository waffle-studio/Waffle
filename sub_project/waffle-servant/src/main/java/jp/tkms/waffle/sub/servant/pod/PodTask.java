package jp.tkms.waffle.sub.servant.pod;

import com.eclipsesource.json.JsonArray;

import java.io.IOException;

public class PodTask {
  public static final String PODTASK = "#PODTASK";
  public Thread shutdownTimer = null;
  boolean isLocalMode;
  int timeout;
  int shutdownTime;
  int marginTime;

  public PodTask(boolean isLocalMode, int timeout, int shutdownTime, int marginTime) {
    this.isLocalMode = isLocalMode;
    this.timeout = timeout;
    this.shutdownTime = shutdownTime;
    this.marginTime = marginTime;
  }

  public PodTask(JsonArray argumentList) {
    this(Boolean.valueOf(argumentList.get(0).asString()), Integer.valueOf(argumentList.get(1).asString()), Integer.valueOf(argumentList.get(2).asString()), Integer.valueOf(argumentList.get(3).asString()));
  }

  public void run() {
    if (shutdownTime < marginTime) {
      throw new RuntimeException("SHUTDOWN PREPARATION MARGIN must be less than FORCE SHUTDOWN TIME");
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
