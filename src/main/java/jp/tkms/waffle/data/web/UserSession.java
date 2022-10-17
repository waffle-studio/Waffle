package jp.tkms.waffle.data.web;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.Database;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.StringFileUtil;
import org.checkerframework.checker.units.qual.A;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSession {
  private static ConcurrentHashMap<String, UserSession> idMap = new ConcurrentHashMap<>();
  private static String waffleId = null;

  private String id;

  public static String getWaffleId() {
    if (waffleId == null) {
      synchronized (idMap) {
        if (Files.exists(Constants.UUID_FILE)) {
          waffleId = StringFileUtil.read(Constants.UUID_FILE).trim();
        } else {
          waffleId = UUID.randomUUID().toString();
          StringFileUtil.write(Constants.UUID_FILE, waffleId.toString());
        }
      }
    }
    return waffleId;
  }

  public UserSession(String sessionId) {
    id = sessionId;
  }

  public static UserSession create() {
    UserSession session = new UserSession(UUID.randomUUID().toString());
    idMap.put(session.id, session);
    return session;
  }

  public String getSessionId() {
    return id;
  }

  public static boolean isContains(String sessionId) {
    return idMap.contains(sessionId);
  }

  public static UserSession getInstance(String id) {
    return idMap.get(id);
  }

  public static ArrayList<UserSession> getList() {
    ArrayList list = new ArrayList();
    for (UserSession session : idMap.values()) {
      list.add(session);
    }
    return list;
  }
}
