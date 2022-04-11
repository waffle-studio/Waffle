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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.webSocket;

@WebSocket
public class PushNotifier {
  static Map<Session, BufferedMessageQueue> sessionMap = new ConcurrentHashMap<>();

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
    unicastMessage(session, "webSocket.send('{\"jobnum\": null}');");
  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {
    sessionMap.remove(session).close();
  }

  @OnWebSocketMessage
  public void onMessage(Session session, String message) {
    JsonObject json = Json.parse(message).asObject();
    for (String name : json.names()) {
      switch (name) {
        case "jobnum":
          unicastMessage(session, createJobNumMessage(ExecutableRunTask.getNum()));
          break;
      }
    }
  }

  @OnWebSocketError
  public void onError(Session session, Throwable error) {
    //NOP
  }

  public static void broadcastMessage(String message) {
    sessionMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
      unicastMessage(session, message);
    });
  }

  public static void unicastMessage(Session session, String message) {
    sessionMap.get(session).add(message);
  }

  public static String createJobNumMessage(int num) {
    if (num > 0) {
      return "el=s_id('jobnum');s_showib(el);s_put(el," + num + ");";
    } else {
      return "s_hide(s_id('jobnum'));";
    }
  }

  public static void sendRubyRunningStatus(boolean status) {
    broadcastMessage("rubyRunningStatus(" + (status ? "true" : "false") + ");");
  }
}
