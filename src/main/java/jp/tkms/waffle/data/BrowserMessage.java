package jp.tkms.waffle.data;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BrowserMessage {
  private static ConcurrentLinkedQueue<BrowserMessage> messageQueue = new ConcurrentLinkedQueue<>();
  private static Long nextId = 0L;

  private long rowId;
  private long timestamp;
  private String message;

  public BrowserMessage(String message) {
    this.rowId = getNextRowId();
    this.timestamp = System.currentTimeMillis();
    this.message = message;
  }

  public static long getNextRowId() {
    synchronized (nextId) {
      return nextId++;
    }
  }

  public static long getCurrentRowId() {
    synchronized (nextId) {
      return nextId;
    }
  }

  public static ArrayList<BrowserMessage> getList(Long currentRowId) {
    Queue<BrowserMessage> queue = new LinkedList<>(messageQueue);

    while (queue.size() > 0 && queue.peek().rowId <= currentRowId) {
      queue.poll();
    }

    return new ArrayList<>(queue);
  }

  public static ArrayList<BrowserMessage> getList(String currentRowId) {
    return getList(Long.valueOf(currentRowId));
  }

  public static void removeExpired() {
    while (messageQueue.size() > 0 && (messageQueue.peek().timestamp + 5000) < System.currentTimeMillis()) {
      messageQueue.poll();
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
