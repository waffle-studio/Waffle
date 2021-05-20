package jp.tkms.waffle.data;

import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.Constants;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class SystemDataAgent {
  public static final String PREFIX = "system.";
  public static final int GB = 1024 * 1024 * 1024;
  private static final HardwareAbstractionLayer HARDWARE = new SystemInfo().getHardware();
  private static final CentralProcessor CENTRAL_PROCESSOR = HARDWARE.getProcessor();
  private static final GlobalMemory GLOBAL_MEMORY = HARDWARE.getMemory();

  private static long[] cpuOldTicks = new long[CentralProcessor.TickType.values().length];

  public static JsonObject request(String name) {
    JsonObject jsonObject = new JsonObject();
    switch (name.substring(PREFIX.length())) {
      case "storage":
        jsonObject.add(name, (int)(Constants.WORK_DIR.toFile().getUsableSpace() / GB));
        break;
      case "cpu":
        //jsonObject.add(name, (int)(100.0 * ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad()));
        jsonObject.add(name, (int)(100.0 * getCpuLoad()));
        break;
      case "memory":
        //jsonObject.add(name, getTotalMemory() - round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize() / GB));
        jsonObject.add(name, getTotalMemory() - round2((double)GLOBAL_MEMORY.getAvailable() / GB));
        break;
    }
    return jsonObject;
  }

  public static double getCpuLoad() {
    double load = CENTRAL_PROCESSOR.getSystemCpuLoadBetweenTicks(cpuOldTicks);
    cpuOldTicks = CENTRAL_PROCESSOR.getSystemCpuLoadTicks();
    return load;
  }

  public static long getTotalStorage() {
    return Constants.WORK_DIR.toFile().getTotalSpace() / GB;
  }

  public static Double getTotalMemory() {
    //return round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / GB);
    return round2((double)GLOBAL_MEMORY.getTotal() / GB);
  }

  static double round2(double v) {
    return (double)(long)(v * 100) / 100;
  }
}
