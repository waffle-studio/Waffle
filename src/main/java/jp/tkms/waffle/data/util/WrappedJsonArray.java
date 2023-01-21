package jp.tkms.waffle.data.util;

import com.eclipsesource.json.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

public class WrappedJsonArray implements List<Object> {
  JsonArray jsonArray;
  private Runnable updateHandler;

  public WrappedJsonArray(JsonArray jsonArray, Runnable updateHandler) {
    this.jsonArray = jsonArray;
    this.updateHandler = updateHandler;
  }

  public WrappedJsonArray(JsonArray jsonArray) {
    this(jsonArray, null);
  }

  public WrappedJsonArray(String jsonText) {
    try {
      JsonValue value = Json.parse(jsonText);
      this.jsonArray = value.isArray() ? value.asArray() : new JsonArray();
    } catch (ParseException | NullPointerException e) {
      this.jsonArray = new JsonArray();
    }
  }

  public WrappedJsonArray(List<Object> list) {
    this();
    addAll(list);
  }

  public WrappedJsonArray() {
    this(new JsonArray());
  }

  private void update() {
    if (updateHandler != null) {
      updateHandler.run();
    }
  }

  @Override
  public String toString() {
    return jsonArray.toString(WriterConfig.MINIMAL);
  }

  public JsonArray toJsonArray() {
    return jsonArray;
  }

  public void writeMinimalFile(Path path) {
    StringFileUtil.write(path, toString());
  }

  @Override
  public int size() {
    return jsonArray.size();
  }

  @Override
  public boolean isEmpty() {
    return jsonArray.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return toArrayList().contains(o);
  }

  @NotNull
  @Override
  public Iterator<Object> iterator() {
    return toArrayList().iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return toArrayList().toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] ts) {
    return toArrayList().toArray(ts);
  }

  @Override
  public boolean add(Object o) {
    jsonArray.add(WrappedJson.toJsonValue(o));
    update();
    return true;
  }

  @Override
  public boolean remove(Object o) {
    int prevSize = size();

    int index = -1;
    while ((index = indexOf(o)) >= 0) {
      jsonArray.remove(index);
    }

    if (prevSize != size()) {
      update();
      return true;
    }
    return false;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> collection) {
    return toArrayList().containsAll(collection);
  }

  @Override
  public boolean addAll(@NotNull Collection<?> collection) {
    collection.stream().forEach(o -> add(o));
    return !collection.isEmpty();
  }

  @Override
  public boolean addAll(int i, @NotNull Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> collection) {
    int prevSize = size();
    collection.stream().forEach(o -> remove(o));
    return prevSize != size();
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> collection) {
    boolean res = jsonArray.values().retainAll(collection);
    return res;
  }

  @Override
  public void clear() {
    IntStream.range(size(), 0).forEach(i -> remove(i));
  }

  @Override
  public Object get(int i) {
    return WrappedJson.toObject(jsonArray.get(i), updateHandler);
  }

  @Override
  public Object set(int i, Object o) {
    jsonArray.set(i, WrappedJson.toJsonValue(o));
    update();
    return o;
  }

  @Override
  public void add(int i, Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(int i) {
    Object object = jsonArray.get(i);
    jsonArray.remove(i);
    update();
    return object;
  }

  @Override
  public int indexOf(Object o) {
    return toArrayList().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return toArrayList().lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<Object> listIterator() {
    return listIterator(0);
  }

  @NotNull
  @Override
  public ListIterator<Object> listIterator(int i) {
    return toArrayList().listIterator(i);
  }

  @NotNull
  @Override
  public List<Object> subList(int i, int i1) {
    return toArrayList().subList(i, i1);
  }

  ArrayList<Object> toArrayList() {
    ArrayList<Object> list = new ArrayList<>();
    IntStream.range(0, size()).forEach(i -> list.add(get(i)));
    return list;
  }
}
