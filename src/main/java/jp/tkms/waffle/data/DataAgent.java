package jp.tkms.waffle.data;

import com.eclipsesource.json.JsonObject;

public class DataAgent {
  public static JsonObject request(String name) {
    if (name.startsWith(SystemDataAgent.PREFIX)) {
      return SystemDataAgent.request(name);
    }
    return new JsonObject();
  }
}
