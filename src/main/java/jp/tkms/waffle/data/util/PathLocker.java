package jp.tkms.waffle.data.util;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public class PathLocker implements Serializable {
  private static InstanceCache<String, PathLocker> lockerMap = new InstanceCache<>();

  private PathLocker() {
  }

  public static PathLocker getLocker(Path path) {
    String normalizedPathString = path.toAbsolutePath().normalize().toString();
    PathLocker locker = lockerMap.get(normalizedPathString);
    if (locker == null) {
      synchronized (lockerMap) {
        locker = lockerMap.get(normalizedPathString);
        if (locker == null) {
          locker = new PathLocker();
          lockerMap.put(normalizedPathString, locker);
        }
      }
    }
    return locker;
  }

  public static void waitAllCachedFiles() {
    HashSet<String> allKeySet = new HashSet<>();
    for (Map.Entry<String, PathLocker> entry : lockerMap.getMap().entrySet()) {
      synchronized (entry.getValue()) {
        allKeySet.add(entry.getKey());
      }
    }
    lockerMap.removeAll(allKeySet);
  }
}
