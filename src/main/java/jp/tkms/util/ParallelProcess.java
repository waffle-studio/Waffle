package jp.tkms.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class ParallelProcess {
  private ExecutorService executorService;
  ArrayList<Future> futureList;

  public ParallelProcess(ExecutorService executorService) {
    this.executorService = executorService;
    this.futureList = new ArrayList<>();
  }

  public void submit(Runnable runnable) {
    futureList.add(executorService.submit(runnable));
  }

  public void waitFor() throws Exception {
    Exception lastException = null;
    for (Future future : futureList) {
      try {
        future.get();
      } catch (Exception e) {
        lastException = e;
      }
    }
    if (lastException != null) {
      throw lastException;
    }
  }

  public static void submitAndWait(ExecutorService executorService, Consumer<ParallelProcess> consumer) throws Exception {
    ParallelProcess process = new ParallelProcess(executorService);
    consumer.accept(process);
    process.waitFor();
  }
}
