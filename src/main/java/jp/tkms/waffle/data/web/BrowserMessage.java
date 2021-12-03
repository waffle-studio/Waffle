package jp.tkms.waffle.data.web;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class BrowserMessage {
  private static ConcurrentLinkedQueue<BrowserMessage> messageQueue = new ConcurrentLinkedQueue<>();
  private static AtomicLong nextId = new AtomicLong(0);

  private long rowId;
  private long timestamp;
  private String message;

  public BrowserMessage(String message) {
    this.rowId = nextId.getAndIncrement();
    this.timestamp = System.currentTimeMillis();
    this.message = message;
  }

  public static long getCurrentRowId() {
    return nextId.get();
  }

  public static ArrayList<BrowserMessage> getList(Long currentRowId) {
    Queue<BrowserMessage> queue = new LinkedList<>(messageQueue);

    while (queue.size() > 0 && queue.peek().rowId <= currentRowId) {
      queue.poll();
    }

    return new ArrayList<>(queue);
  }

  public static void removeExpired() {
    try {
      while (messageQueue.size() > 0 && (messageQueue.peek().timestamp + 10000) < System.currentTimeMillis()) {
        messageQueue.poll();
      }
    } catch (Exception e) {
      //NP : the case where exceptions occurs is when the DB is broken
    }
  }

  public static void addMessage(String message) {
    removeExpired();
    messageQueue.add(new BrowserMessage("try{" + message + "}catch(e){}"));
  }

  public long getRowId() {
    return rowId;
  }

  public String getMessage() {
    return message;
  }
}
