package jp.tkms.waffle.data.internal.guard;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Guard {
  public static final String RUN_PATH = "path";
  public static final String OPERATOR = "operator";

  private JsonObject jsonObject;

  public Guard() {
    //temporary
  }

  public Guard(String jsonString) {
    // todo: currently, this class is not used
    jsonObject = Json.parse(jsonString).asObject();
  }

  @Override
  public String toString() {
    return jsonObject.toString();
  }
}
