package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

public class EasyBench {
  int THRESHOLD = 1000;

  long time = Long.MAX_VALUE;
  boolean isRejected = false;

  public EasyBench() {
    run();
  }

  private void run() {
    long start = System.currentTimeMillis();

    /*
    ArrayList<Long> tmp1 = new ArrayList<>();
    long tmp2 = 0;
    for (int i = 0; i < 10000000; i += 1) {
      tmp2 += i;
      tmp1.add(tmp2);
    }
    for (Long v : tmp1) {
      tmp2 -= v;
    }

    double tmp3 = 0.0;
    for (int i = 0; i < 10000000; i += 1) {
      tmp3 += Math.atan(i);
    }
    double tmp4 = tmp3 * tmp3;
     */

    AtomicReference<String> tmp5 = new AtomicReference<>("");
    try {
      //Files.list(Paths.get("/")).forEach(path -> tmp5.updateAndGet(v -> v + path));
      Files.list(Paths.get("/tmp")).forEach(path -> tmp5.updateAndGet(v -> v + path));
    } catch (IOException e) {
      // NOP
    }

    time = System.currentTimeMillis() - start;
    isRejected = (time >= THRESHOLD);
  }

  public boolean isRejected() {
    return isRejected;
  }

  public void print() {
    System.out.println(time + (isRejected() ? " REJECTED" : ""));
  }
}
