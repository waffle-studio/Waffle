package jp.tkms.util;

public class ObjectWrapper<T> extends Object {
  private T value;

  public ObjectWrapper(T value) {
    this.value = value;
  }

  public ObjectWrapper() {
    this(null);
  }

  public void setValue(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }
}
