package jp.tkms.waffle.data.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import jp.tkms.util.Values;

public class JsonUtil {
  public static JsonValue toValue(Object object) {
    if (object instanceof String) {
      return Json.value((String) object);
    } else {
      return Json.parse(object.toString());
    }
  }

  public static Object toObject(JsonValue value) {
    if (value.isNumber()) {
      return Values.convertString(value.toString());
    } else if (value.isBoolean()) {
      if (value.isTrue()) {
        return Boolean.TRUE;
      }
      return Boolean.FALSE;
    } else if (value.isNull()) {
      return null;
    } else if (value.isString()) {
      return value.asString();
    }
    return value;
  }
}
