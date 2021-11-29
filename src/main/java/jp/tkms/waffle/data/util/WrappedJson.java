package jp.tkms.waffle.data.util;

import com.eclipsesource.json.JsonObject;

public class WrappedJson extends JsonObject {
  public WrappedJson(JsonObject jsonObject) {
    super(jsonObject);
  }

}
