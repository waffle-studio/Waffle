package jp.tkms.waffle.data;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jp.tkms.waffle.Constants;

import java.lang.management.ManagementFactory;

public class SystemDataAgent {
  public static final String PREFIX = "system.";
  public static final int GB = 1024 * 1024 * 1024;

  public static JsonObject request(String name) {
    JsonObject jsonObject = new JsonObject();
    switch (name.substring(PREFIX.length())) {
      case "storage":
        jsonObject.add(name, (int)(Constants.WORK_DIR.toFile().getUsableSpace() / GB));
        break;
      case "cpu":
        jsonObject.add(name, (int)(100.0 * ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad()));
        break;
      case "memory":
        jsonObject.add(name, getTotalMemory() - round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize() / GB));
        break;
    }
    return jsonObject;
  }

  public static long getTotalStorage() {
    return Constants.WORK_DIR.toFile().getTotalSpace() / GB;
  }

  public static Double getTotalMemory() {
    return round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / GB);
  }

  static double round2(double v) {
    return (double)(long)(v * 100) / 100;
  }
}
