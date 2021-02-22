package jp.tkms.waffle.data.util;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InstanceCache<K extends Object, V extends Object> {
  private static ArrayList<InstanceCache> cacheList = new ArrayList<>();

  private Map<K, WeakReference<V>> map = new ConcurrentHashMap<>();

  public InstanceCache() {
    cacheList.add(this);
  }

  public V get(K key) {
    if (map.containsKey(key)) {
      return map.get(key).get();
    }
    return null;
  }

  public V getOrCreate(K key, Function<K, V> creator) {
    V instance = get(key);
    if (instance == null) {
      instance = creator.apply(key);
      if (instance != null) {
        put(key, instance);
      }
    }
    return instance;
  }

  public V put(K key, V value) {
    map.put(key, new WeakReference<>(value));
    return value;
  }

  public void remove(K key) {
    map.remove(key);
  }

  public int size() {
    return map.size();
  }

  public Set<K> keySet() {
    return map.keySet();
  }

  public void removeAll(Collection<K> list) {
    for (K key : list) {
      remove(key);
    }
  }

  public void clear() {
    map.clear();
  }

  public static void gc() {
    System.gc();
    ArrayList<Object> removingKeyList = new ArrayList();
    for (InstanceCache cache : cacheList) {
      synchronized (cache) {
        removingKeyList.clear();
        for (Object entry : cache.map.entrySet()) {
          WeakReference<Object> reference = ((Map.Entry<Object, WeakReference<Object>>)entry).getValue();
          if (reference == null || reference.get() == null) {
            removingKeyList.add(((Map.Entry<Object, WeakReference<Object>>)entry).getKey());
          }
        }
        cache.removeAll(removingKeyList);
      }
    }
  }

  public static String debugReport() {
    int count = 0;
    for (InstanceCache cache : cacheList) {
      count += cache.size();
    }
    return InstanceCache.class.getSimpleName() + ": " + count;
  }
}
