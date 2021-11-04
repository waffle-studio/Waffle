package jp.tkms.waffle.manager;

import jp.tkms.waffle.data.internal.guard.ValueGuard;

public class Filter {
  private String filterOperator;
  private String filterValue;

  public Filter(String filterOperator, String filterValue) {
    this.filterOperator = filterOperator;
    this.filterValue = filterValue;
  }

  public Filter(ValueGuard valueGuard) {
    this(valueGuard.getOperator(), valueGuard.getValue());
  }

  public boolean apply(Object value) {
    if (value == null) {
      if (filterValue.toLowerCase().equals("null") && filterOperator.equals("==")) {
        return true;
      } else if (filterOperator.equals("!=")) {
        return true;
      }
    } else {
      try {
        double value1 = Double.valueOf(value.toString());
        double value2 = Double.valueOf(filterValue);
        switch (filterOperator) {
          case "==":
            return value1 == value2;
          case "!=":
            return value1 != value2;
          case "<=":
            return value1 <= value2;
          case ">=":
            return value1 >= value2;
          case "<":
            return value1 < value2;
          case ">":
            return value1 > value2;
        }
      } catch (NumberFormatException e) {
        if (filterValue.toLowerCase().equals("true")) {
          switch (filterOperator) {
            case "==":
              return value.toString().toLowerCase().equals("true");
            case "!=":
              return value.toString().toLowerCase().equals("false");
          }
        } else if (filterValue.toLowerCase().equals("false")) {
          switch (filterOperator) {
            case "==":
              return value.toString().toLowerCase().equals("false");
            case "!=":
              return value.toString().toLowerCase().equals("true");
          }
        }
        switch (filterOperator) {
          case "==":
            return value.toString().equals(filterValue);
          case "!=":
            return !value.toString().equals(filterValue);
        }
      }
    }
    return false;
  }
}
