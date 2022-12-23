package jp.tkms.waffle.sub.servant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class EasyBench {
  int THRESHOLD = 3000;

  long time = Long.MAX_VALUE;
  boolean isRejected = false;

  public EasyBench() {
    run();
  }

  private void run() {
    long start = System.currentTimeMillis();

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

    String tmp5 = "";
    for (int i = 0; i < 1000; i += 1) {
      tmp5 += i;
    }
    String tmp6 = tmp5 + tmp5;

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
