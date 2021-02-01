package jp.tkms.waffle.data.util;


import org.ehcache.Cache;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashSet;

public class PathLocker extends Object implements Serializable {
  private static Cache<String, PathLocker> lockerMap = new InstanceCache<PathLocker>(PathLocker.class, 2000, 3600).getCacheStore();

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
    for (Cache.Entry<String, PathLocker> entry : lockerMap) {
      synchronized (entry.getValue()) {
        allKeySet.add(entry.getKey());
      }
    }
    lockerMap.removeAll(allKeySet);
  }
}
