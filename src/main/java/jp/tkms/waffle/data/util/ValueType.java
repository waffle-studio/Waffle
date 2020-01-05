package jp.tkms.waffle.data.util;

public enum ValueType {
  String(1), Integer(2), Double(3), Boolean(4);

  private final int id;

  ValueType(final int id) {
    this.id = id;
  }

  public int toInt() { return id; }

  public static ValueType valueOf(int i) {
    for (ValueType valueType : ValueType.values()) {
      if (valueType.toInt() == i) { return valueType; }
    }
    return null;
  }
}
