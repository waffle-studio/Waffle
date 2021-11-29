package jp.tkms.waffle.data.util;

import com.eclipsesource.json.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WrappedJsonArray implements List<Object> {
  JsonArray jsonArray;

  public WrappedJsonArray(JsonArray jsonArray) {
    this.jsonArray = jsonArray;
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
    return true;
  }

  @Override
  public boolean remove(Object o) {
    return jsonArray.values().remove(o);
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
    return jsonArray.values().removeAll(collection);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> collection) {
    return jsonArray.values().retainAll(collection);
  }

  @Override
  public void clear() {
    jsonArray = new JsonArray();
  }

  @Override
  public Object get(int i) {
    return WrappedJson.toObject(jsonArray.get(i));
  }

  @Override
  public Object set(int i, Object o) {
    jsonArray.set(i, WrappedJson.toJsonValue(o));
    return o;
  }

  @Override
  public void add(int i, Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(int i) {
    return jsonArray.values().remove(i);
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
    return Arrays.stream(jsonArray.values().toArray()).iterator();
  }

  @NotNull
  @Override
  public ListIterator<Object> listIterator(int i) {
    return null;
  }

  @NotNull
  @Override
  public List<Object> subList(int i, int i1) {
    return null;
  }
}
