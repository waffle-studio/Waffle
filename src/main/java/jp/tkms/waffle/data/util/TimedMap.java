package jp.tkms.waffle.data.util;

import java.util.*;

public class TimedMap<K,V> {
  public static final int DEFAULT_EXPIRY_SECOND = 3600;

  private HashMap<K,Instance<V>> instanceMap = new HashMap<>();
  private Deque<K> removingQueue = new LinkedList<>();
  private int expiry;

  public TimedMap(int expirySec) {
    expiry = expirySec;
  }

  public TimedMap() {
    this(DEFAULT_EXPIRY_SECOND);
  }

  public V get(K key) {
    synchronized (this) {
      if (instanceMap.containsKey(key)) {
        if (!key.equals(removingQueue.peekLast())) {
          removingQueue.remove(key);
          removingQueue.addLast(key);
        }
        return (V) instanceMap.get(key).updateTimestamp().getEntity();
      } else{
        return null;
      }
    }
  }

  public boolean put(K key, V instance) {
    removeExpired();
    synchronized (this) {
      if (instanceMap.containsKey(key)) {
        return false;
      } else{
        instanceMap.put(key, new Instance<>(instance));
        return true;
      }
    }
  }

  public HashMap<K, V> getMap() {
    synchronized (this) {
      HashMap<K,V> map = new HashMap<>();
      for (Map.Entry<K,Instance<V>> entry : instanceMap.entrySet()) {
        map.put(entry.getKey(), entry.getValue().getEntity());
      }
      return map;
    }
  }

  public void removeAll(Collection<K> keyList) {
    synchronized (this) {
      for (K key : keyList) {
        instanceMap.remove(key);
        removingQueue.remove(key);
      }
    }
  }

  private void removeExpired() {
    synchronized (this) {
      while (!removingQueue.isEmpty() && instanceMap.get(removingQueue.peekFirst()).isExpired()) {
        instanceMap.remove(removingQueue.pollFirst());
      }
    }
  }

  class Instance<V> {
    long lastAccess;
    V entity;

    Instance(V instance) {
      entity = instance;
      updateTimestamp();
    }

    Instance updateTimestamp() {
      lastAccess = System.currentTimeMillis();
      return this;
    }

    V getEntity() {
      return entity;
    }

    boolean isExpired() {
      return lastAccess + (expiry * 1000) < System.currentTimeMillis();
    }
  }
}
