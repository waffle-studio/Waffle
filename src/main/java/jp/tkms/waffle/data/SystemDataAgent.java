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

  private static HardwareAbstractionLayer hardware = null;
  private static long[] cpuOldTicks = new long[CentralProcessor.TickType.values().length];

  public static JsonObject request(String name) {
    JsonObject jsonObject = new JsonObject();
    switch (name.substring(PREFIX.length())) {
      case "storage":
        try {
          jsonObject.add(name, (int)(Constants.WORK_DIR.toFile().getUsableSpace() / GB));
        } catch (NullPointerException e) {
          jsonObject.add(name, -1);
        }
        break;
      case "cpu":
        //jsonObject.add(name, (int)(100.0 * ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad()));
        try {
          jsonObject.add(name, (int)(100.0 * getCpuLoad()));
        } catch (NullPointerException e) {
          jsonObject.add(name, -1);
        }
        break;
      case "memory":
        //jsonObject.add(name, getTotalMemory() - round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize() / GB));
        try {
          jsonObject.add(name, round2(getTotalMemory() - (double)getHardware().getMemory().getAvailable() / GB));
        } catch (NullPointerException e) {
          jsonObject.add(name, -1);
        }
        break;
    }
    return jsonObject;
  }

  public static HardwareAbstractionLayer getHardware() {
    if (hardware == null) {
      try {
        hardware = new SystemInfo().getHardware();
      } catch (NoClassDefFoundError e) {
        return null;
      }
    }
    return hardware;
  }

  public static double getCpuLoad() {
    try {
      CentralProcessor centralProcessor = getHardware().getProcessor();
      double load = centralProcessor.getSystemCpuLoadBetweenTicks(cpuOldTicks);
      cpuOldTicks = centralProcessor.getSystemCpuLoadTicks();
      return load;
    } catch (Exception e) {
      return -1;
    }
  }

  public static long getTotalStorage() {
    return Constants.WORK_DIR.toFile().getTotalSpace() / GB;
  }

  public static double getTotalMemory() {
    try {
      //return round2((double)((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / GB);
      return round2((double)getHardware().getMemory().getTotal() / GB);
    } catch (Exception e) {
      return -1;
    }
  }

  static double round2(double v) {
    return (double)(long)(v * 100) / 100;
  }
}
