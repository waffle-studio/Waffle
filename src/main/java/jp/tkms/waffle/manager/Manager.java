package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.project.workspace.Workspace;

import java.util.concurrent.atomic.AtomicInteger;

public class Manager {
  private Workspace workspace;
  private AtomicInteger waitingCount;
  private Thread processorThread = null;

  Manager(Workspace workspace) {
    this.workspace = workspace;
    this.waitingCount = new AtomicInteger(0);
  }

  public void update() {
    waitingCount.incrementAndGet();

    synchronized (this) {
      if (processorThread == null) {
        processorThread = new Thread(() -> updateProcess());
        processorThread.start();
      }
    }
  }

  void updateProcess() {
    while (waitingCount.getAndDecrement() > 0) {

    }
    synchronized (this) {
      processorThread = null;
    }
  }

}
