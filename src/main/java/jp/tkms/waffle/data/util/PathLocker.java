package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

public class PathLocker {
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
    for (String key : lockerMap.keySet()) {
      synchronized (getLocker(Paths.get(key))) {
        allKeySet.add(key);
      }
    }
    lockerMap.removeAll(allKeySet);
  }
}
