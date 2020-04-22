package jp.tkms.waffle.data.util;

public class TypedValue {
  public ValueType type;
  public Object value;

  public TypedValue(ValueType type, Object value) {
    this.type = type;
    this.value = value;
  }
}
