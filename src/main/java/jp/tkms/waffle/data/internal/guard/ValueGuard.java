package jp.tkms.waffle.data.internal.guard;

public class ValueGuard {
  String[] slicedGuard;

  public ValueGuard(String guard) throws InsufficientStatementException, InvalidOperatorException {
    slicedGuard = guard.split(" ", 4);

    if (slicedGuard.length != 4) {
      throw new InsufficientStatementException(guard);
    }

    switch (getOperator()) {
      case "==":
      case "!=":
      case "<=":
      case ">=":
      case "<":
      case ">":
        break;
      default:
        throw new InvalidOperatorException(guard);
    }
  }

  public String getTargetRunPath() {
    return slicedGuard[0];
  }

  public String getKey() {
    return slicedGuard[1];
  }

  public String getOperator() {
    return slicedGuard[2];
  }

  public String getValue() {
    return slicedGuard[3];
  }

  public static class InsufficientStatementException extends Exception {
    public InsufficientStatementException(String guard) {
      super("Insufficient statement: " + guard);
    }
  }

  public static class InvalidOperatorException extends Exception {
    public InvalidOperatorException(String guard) {
      super("Invalid operator: " + guard);
    }
  }
}
