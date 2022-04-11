package jp.tkms.waffle.web.component.websocket;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class BufferedMessageQueue extends ConcurrentLinkedQueue<String> {
  private int wait;
  private Thread thread;
  private boolean isClosed = false;
  private Consumer<String> messageConsumer;

  public BufferedMessageQueue(Consumer<String> consumer) {
    messageConsumer = consumer;
  }

  void close() {
    isClosed = true;
  }

  @Override
  public boolean add(String s) {
    synchronized (this) {
      if (isClosed) { return false; }
      wait = 200;
      if (thread == null) {
        thread = new Thread(() -> {
          do {
            try {
              Thread.sleep(100);
              wait -= 100;
            } catch (InterruptedException e) {
              return;
            }
          } while (wait > 0);
          flush();
        });
        thread.start();
      }
      return super.add(s);
    }
  }

  public void flush() {
    synchronized (this) {
      if (isClosed) { return; }
      if (size() == 1) {
        messageConsumer.accept(poll());
      } else {
        String message = "";
        while (peek() != null) {
          message += "try{" + poll() + "}catch(e){};";
        }
        messageConsumer.accept(message);
      }
      thread = null;
    }
  }
}
