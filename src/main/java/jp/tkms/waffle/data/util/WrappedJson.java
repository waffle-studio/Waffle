package jp.tkms.waffle.data.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jp.tkms.util.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WrappedJson implements Map<String, Object> {
  private JsonObject jsonObject;

  public WrappedJson(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public static JsonValue toJsonValue(Object object) {
    if (object instanceof String) {
      return Json.value((String) object);
    } else {
      return Json.parse(object.toString());
    }
  }

  public static Object toObject(JsonValue value) {
    if (value == null) {
      return null;
    } else if (value.isNumber()) {
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
    } else if (value.isArray()) {
      return new WrappedJsonArray(value.asArray());
    } else if (value.isObject()) {
      return new WrappedJson(value.asObject());
    }
    return value;
  }

  @Override
  public Object get(Object key) {
    return toObject(jsonObject.get(key.toString()));
  }

  @Override
  public int size() {
    return jsonObject.size();
  }

  @Override
  public boolean isEmpty() {
    return jsonObject.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    return jsonObject.names().contains(o);
  }

  @Override
  public boolean containsValue(Object o) {
    for (JsonObject.Member member : jsonObject) {
      if (member.getValue().equals(o)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public Object put(String key, Object value) {
    jsonObject.add(key, toJsonValue(value));
    return value;
  }

  @Override
  public Object remove(Object key) {
    Object object = get(key);
    if (object != null) {
      jsonObject.remove(key.toString());
    }
    return object;
  }

  @Override
  public void putAll(@NotNull Map<? extends String, ?> map) {
    for (Map.Entry entry : map.entrySet()) {
      put(entry.getKey().toString(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    jsonObject = new JsonObject();
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    return new HashSet<>(jsonObject.names());
  }

  @NotNull
  @Override
  public Collection<Object> values() {
    ArrayList list = new ArrayList();
    for (JsonObject.Member member : jsonObject) {
      list.add(member.getValue());
    }
    return list;
  }

  @Override
  public Set<Map.Entry<String, Object>> entrySet() {
    Set<Map.Entry<String, Object>> set = new HashSet<>();
    for (JsonObject.Member member : jsonObject) {
      set.add(new Entry(member));
    }
    return set;
  }

  public class Entry implements Map.Entry<String, Object> {
    JsonObject.Member member;

    public Entry(JsonObject.Member member) {
      this.member = member;
    }

    @Override
    public String getKey() {
      return member.getName();
    }

    @Override
    public Object getValue() {
      return get(getKey());
    }

    @Override
    public Object setValue(Object object) {
      return put(getKey(), object);
    }
  }
}
