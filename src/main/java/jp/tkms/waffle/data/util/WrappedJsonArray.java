package jp.tkms.waffle.data.util;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.WriterConfig;
import org.jetbrains.annotations.NotNull;

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
    return jsonArray.values().contains(o);
  }

  @NotNull
  @Override
  public Iterator<Object> iterator() {
    return Arrays.stream(jsonArray.values().toArray()).iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return jsonArray.values().toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] ts) {
    return jsonArray.values().toArray(ts);
  }

  @Override
  public boolean add(Object o) {
    jsonArray.add(WrappedJson.toJsonValue(o));
    update();
    return true;
  }

  @Override
  public boolean remove(Object o) {
    boolean res = jsonArray.values().remove(o);
    update();
    return res;
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> collection) {
    return jsonArray.values().containsAll(collection);
  }

  @Override
  public boolean addAll(@NotNull Collection<?> collection) {
    for (Object o : collection) {
      add(o);
    }
    return true;
  }

  @Override
  public boolean addAll(int i, @NotNull Collection<?> collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> collection) {
    boolean res = jsonArray.values().removeAll(collection);
    return res;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> collection) {
    boolean res = jsonArray.values().retainAll(collection);
    return res;
  }

  @Override
  public void clear() {
    IntStream.range(0, size()).forEach(i -> remove(i));
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
    Object object = jsonArray.values().remove(i);
    update();
    return object;
  }

  @Override
  public int indexOf(Object o) {
    return jsonArray.values().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return jsonArray.values().lastIndexOf(o);
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
    return new ArrayList<Object>(jsonArray.values());
  }
}
