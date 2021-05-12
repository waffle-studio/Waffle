package jp.tkms.waffle.data;

import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.Constants;

import java.lang.management.ManagementFactory;

public class SystemDataAgent {
  public static final String PREFIX = "system.";

  public static JsonObject request(String name) {
    JsonObject jsonObject = new JsonObject();
    switch (name.substring(PREFIX.length())) {
      case "storage":
        jsonObject.add(name, Constants.WORK_DIR.toFile().getUsableSpace());
        break;
      case "cpu":
        jsonObject.add(name, ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad());
        break;
      case "memory":
        jsonObject.add(name, ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize());
        break;
    }
    return jsonObject;
  }
}
