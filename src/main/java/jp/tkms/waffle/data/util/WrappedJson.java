package jp.tkms.waffle.data.util;

import afu.org.checkerframework.checker.oigj.qual.O;
import com.eclipsesource.json.*;
import jp.tkms.util.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jruby.RubySymbol;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class WrappedJson implements Map<Object, Object> {
  private JsonObject jsonObject;
  private Runnable updateHandler;

  public WrappedJson(JsonObject jsonObject, Runnable updateHandler) {
    this.jsonObject = jsonObject;
    this.updateHandler = updateHandler;
  }

  public WrappedJson(JsonObject jsonObject) {
    this(jsonObject, null);
  }

  public WrappedJson() {
    this(new JsonObject());
  }

  public WrappedJson(String jsonText) {
    try {
      JsonValue value = Json.parse(jsonText);
      this.jsonObject = value.isObject() ? value.asObject() : new JsonObject();
    } catch (ParseException | NullPointerException e) {
      this.jsonObject = new JsonObject();
    }
  }

  public WrappedJson withUpdateHandler(Consumer<WrappedJson> handler) {
    updateHandler = () -> {handler.accept(this);};
    return this;
  }

  private void update() {
    if (updateHandler != null) {
      updateHandler.run();
    }
  }

  @Override
  public String toString() {
    return jsonObject.toString(WriterConfig.MINIMAL);
  }

  public WrappedJson clone() {
    return new WrappedJson(toString());
  }

  public JsonObject toJsonObject() {
    return jsonObject;
  }

  public String toPrettyString() {
    return jsonObject.toString(WriterConfig.PRETTY_PRINT);
  }

  public void writeMinimalFile(Path path) {
    StringFileUtil.write(path, toString());
  }

  public void writePrettyFile(Path path) {
    StringFileUtil.write(path, toPrettyString());
  }

  public WrappedJson merge(WrappedJson wrappedJson) {
    if (wrappedJson == null) {
      throw new NullPointerException("object is null");
    } else {
      for (Object key : wrappedJson.keySet()) {
        Object value = wrappedJson.get(key);
        if (value instanceof WrappedJson) {
          Object currentValue = get(key);
          if (currentValue instanceof WrappedJson) {
            ((WrappedJson)currentValue).merge((WrappedJson)value);
          } else {
            put(key, value);
          }
        } else {
          put(key, value);
        }
      }

      return this;
    }
  }

  public static JsonValue toJsonValue(Object object) {
    if (object instanceof String) {
      return Json.value((String) object);
    } else if (object instanceof Map) {
      WrappedJson wrappedJson = new WrappedJson();
      wrappedJson.putAll((Map)object);
      return wrappedJson.toJsonObject();
    } else if (object instanceof List) {
      WrappedJsonArray wrappedJsonArray = new WrappedJsonArray();
      ((List) object).forEach((value) -> {
        wrappedJsonArray.add(value);
      });
      return wrappedJsonArray.toJsonArray();
    } else {
      return Json.parse(object.toString());
    }
  }

  public static Object toObject(JsonValue value, Runnable updateHandler) {
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
      return new WrappedJsonArray(value.asArray(), updateHandler);
    } else if (value.isObject()) {
      return new WrappedJson(value.asObject(), updateHandler);
    }
    return value;
  }

  @Override
  public Object get(Object key) {
    return toObject(jsonObject.get(key.toString()), () -> {update();});
  }

  public <T> T get(String key, T defaultValue, Function<Object, T> convertor) {
    try {
      if (convertor != null) {
        return containsKey(key) ? convertor.apply(get(key)) : defaultValue;
      } else {
        return containsKey(key) ? (T)get(key) : defaultValue;
      }
    } catch (ClassCastException | NullPointerException e) {
      return defaultValue;
    }
  }

  public Integer getInt(String key, Integer defaultValue) {
    return get(key, defaultValue, v -> Integer.valueOf(v.toString()));
  }

  public Long getLong(String key, Long defaultValue) {
    return get(key, defaultValue, v -> Long.valueOf(v.toString()));
  }

  public Float getFloat(String key, Float defaultValue) {
    return get(key, defaultValue, v -> Float.valueOf(v.toString()));
  }

  public Double getDouble(String key, Double defaultValue) {
    return get(key, defaultValue, v -> Double.valueOf(v.toString()));
  }

  public Boolean getBoolean(String key, Boolean defaultValue) {
    return get(key, defaultValue, null);
  }

  public String getString(String key, String defaultValue) {
    return get(key, defaultValue, v -> v.toString());
  }

  public WrappedJson getObject(String key, WrappedJson defaultValue) {
    return get(key, defaultValue, null);
  }

  public WrappedJsonArray getArray(String key, WrappedJsonArray defaultValue) {
    return get(key, defaultValue, null);
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
  public Object put(Object key, Object value) {
    jsonObject.remove(key.toString());
    jsonObject.add(key.toString(), toJsonValue(value));
    update();
    return value;
  }

  @Override
  public Object remove(Object key) {
    Object object = get(key);
    if (object != null) {
      jsonObject.remove(key.toString());
    }
    update();
    return object;
  }

  @Override
  public void putAll(@NotNull Map<? extends Object, ?> map) {
    for (Map.Entry entry : map.entrySet()) {
      put(entry.getKey().toString(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    keySet().stream().forEach(k -> remove(k));
    update();
  }

  @NotNull
  @Override
  public Set<Object> keySet() {
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
  public Set<Map.Entry<Object, Object>> entrySet() {
    Set<Map.Entry<Object, Object>> set = new HashSet<>();
    for (JsonObject.Member member : jsonObject) {
      set.add(new Entry(member));
    }
    return set;
  }

  public class Entry implements Map.Entry<Object, Object> {
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
