package jp.tkms.waffle.web.component.websocket;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.web.UserSession;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;

import static spark.Spark.webSocket;

@WebSocket
public class PushNotifier {
  public static final String JOBNUM = "jobnum";
  public static final String SUBSCRIBE = "subscribe";

  static Map<Session, BufferedMessageQueue> sessionMap = new ConcurrentHashMap<>();

  static Map<String, List<Session>> subscribingMap = new ConcurrentHashMap<>();
  static Map<Session, List<String>> reversedSubscribingMap = new ConcurrentHashMap<>();

  public static void register() {
    webSocket("/push", PushNotifier.class);
  }

  @OnWebSocketConnect
  public void onConnect(Session session) throws Exception {
    //session.setIdleTimeout(-1);
    sessionMap.put(session, new BufferedMessageQueue((m) -> {
      try {
        session.getRemote().sendString(m);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }));

    boolean passed = false;
    for (HttpCookie cookie : session.getUpgradeRequest().getCookies()) {
      if (cookie.getName().equals(UserSession.getWaffleId()) && UserSession.isContains(cookie.getValue())) {
        passed = true;
      }
    }
    if (!passed) {
      session.disconnect();
    }

    // initial
    JsonObject json = Json.object();
    unicastMessage(session, "webSocket.send('{\"" + SUBSCRIBE + "\": \"URI:' + waffle_uri + '\", \"" + JOBNUM + "\": null}');");
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    sessionMap.remove(session).close();
    unsubscribe(session);
  }

  @OnWebSocketMessage
  public void onMessage(Session session, String message) {
    JsonObject json = Json.parse(message).asObject();
    for (String name : json.names()) {
      switch (name) {
        case SUBSCRIBE:
          subscribe(session, json.getString(SUBSCRIBE, ""));
          break;
        case JOBNUM:
          unicastMessage(session, createJobNumMessage(ExecutableRunTask.getNum()));
          break;
      }
    }
  }

  @OnWebSocketError
  public void onError(Session session, Throwable error) {
    //NOP
  }

  protected void subscribe(Session session, String key) {
    synchronized (subscribingMap) {
      List<Session> sessionList = subscribingMap.get(key);
      if (sessionList == null) {
        sessionList = new LinkedList<>();
        subscribingMap.put(key, sessionList);
      }
      if (!sessionList.contains(session)) {
        sessionList.add(session);
      }

      List<String> keyList = reversedSubscribingMap.get(session);
      if (keyList == null) {
        keyList = new LinkedList<>();
        reversedSubscribingMap.put(session, keyList);
      }
      if (!keyList.contains(key)) {
        keyList.add(key);
      }
    }
  }

  protected void unsubscribe(Session session) {
    synchronized (subscribingMap) {
      if (reversedSubscribingMap.containsKey(session)) {
        for (String key : reversedSubscribingMap.get(session)) {
          List<Session> sessionList = subscribingMap.get(key);
          sessionList.remove(key);
          if (sessionList.size() <= 0) {
            subscribingMap.remove(key);
          }
        }
        reversedSubscribingMap.remove(session);
      }
    }
  }

  public static void broadcastMessage(String message) {
    sessionMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
      unicastMessage(session, message);
    });
  }

  public static void broadcastMessage(String key, String message) {
    List<Session> sessionList = subscribingMap.get(key);
    if (sessionList != null) {
      sessionList.stream().filter(Session::isOpen).forEach(session -> {
        unicastMessage(session, message);
      });
    }
  }

  public static void unicastMessage(Session session, String message) {
    sessionMap.get(session).add(message);
  }

  private static String createJobNumMessage(int num) {
    if (num > 0) {
      return "el=s_id('jobnum');s_showib(el);s_put(el," + num + ");";
    } else {
      return "s_hide(s_id('jobnum'));";
    }
  }

  public static void sendJobNumMessage(int num) {
    broadcastMessage(createJobNumMessage(num));
  }

  public static void sendRubyRunningStatus(boolean status) {
    broadcastMessage("rubyRunningStatus(" + (status ? "true" : "false") + ");");
  }

  public static void sendReloadIfSameUriMessage(String uri) {
    broadcastMessage("URI:" + uri, "location.reload();");
  }
}
