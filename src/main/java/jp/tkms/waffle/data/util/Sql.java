package jp.tkms.waffle.data.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Sql {
  private Database database;
  protected ArrayList<TypedValue> valueList = new ArrayList<>();

  public Sql(Database database) {
    this.database = database;
  }

  private PreparedStatement getPreparedStatement() throws SQLException {
    PreparedStatement preparedStatement = database.preparedStatement(this.toString());

    for (int i = 0; i < valueList.size(); i++) {
      TypedValue typedValue = valueList.get(i);
      switch (typedValue.type) {
        case String:
          preparedStatement.setString(i +1, typedValue.value.toString());
          break;
        case Integer:
          preparedStatement.setInt(i +1, Integer.parseInt(typedValue.value.toString()));
          break;
        case Double:
          preparedStatement.setDouble(i +1, Double.parseDouble(typedValue.value.toString()));
          break;
        case Boolean:
          preparedStatement.setBoolean(i +1, Boolean.parseBoolean(typedValue.value.toString()));
          break;
      }
    }

    return preparedStatement;
  }

  public ResultSet executeQuery() throws SQLException {
    return getPreparedStatement().executeQuery();
  }

  public void execute() throws SQLException {
    getPreparedStatement().execute();
  }

  public static class Value {
    String valueSql = "";
    ArrayList<String> keyList = new ArrayList<>();
    ArrayList<TypedValue> valueList = new ArrayList<>();

    private Value(String valueSql, ArrayList<String> keyList, ArrayList<TypedValue> valueList) {
      this.valueSql = valueSql;
      this.keyList.addAll(keyList);
      this.valueList.addAll(valueList);
    }

    private Value(String valueSql, String key, TypedValue value) {
      this.valueSql = valueSql;
      this.keyList.add(key);
      this.valueList.add(value);
    }

    @Override
    public String toString() {
      return valueSql;
    }

    public ArrayList<TypedValue> getValueList() {
      return valueList;
    }

    public ArrayList<String> getKeyList() {
      return keyList;
    }

    /* equal */
    public static Value equal(String key, String value) {
      return new Value(key + "=?", key, new TypedValue(ValueType.String, value));
    }

    public static Value equal(String key, int value) {
      return new Value(key + "=?", key, new TypedValue(ValueType.Integer, value));
    }

    public static Value equal(String key, double value) {
      return new Value(key + "=?", key, new TypedValue(ValueType.Double, value));
    }

    public static Value equal(String key, boolean value) {
      return new Value(key + "=?", key, new TypedValue(ValueType.Boolean, value));
    }

    /* lessThan */
    public static Value lessThan(String key, String value) {
      return new Value(key + "<?", key, new TypedValue(ValueType.String, value));
    }

    public static Value lessThan(String key, int value) {
      return new Value(key + "<?", key, new TypedValue(ValueType.Integer, value));
    }

    public static Value lessThan(String key, double value) {
      return new Value(key + "<?", key, new TypedValue(ValueType.Double, value));
    }

    public static Value lessThan(String key, boolean value) {
      return new Value(key + "<?", key, new TypedValue(ValueType.Boolean, value));
    }

    /* greeterThan */
    public static Value greeterThan(String key, String value) {
      return new Value(key + ">?", key, new TypedValue(ValueType.String, value));
    }

    public static Value greeterThan(String key, int value) {
      return new Value(key + ">?", key, new TypedValue(ValueType.Integer, value));
    }

    public static Value greeterThan(String key, double value) {
      return new Value(key + ">?", key, new TypedValue(ValueType.Double, value));
    }

    public static Value greeterThan(String key, boolean value) {
      return new Value(key + ">?", key, new TypedValue(ValueType.Boolean, value));
    }

    public static Value and(Value... values) {
      ArrayList<String> keyList = new ArrayList<>();
      ArrayList<TypedValue> valueList = new ArrayList<>();
      String sqlParts = "(";
      for (int i = 0; i < values.length; i++) {
        String value = values[i].toString();
        if (value != null) {
          sqlParts += (i > 0 ? " and " : "") + value;
          keyList.addAll(values[i].getKeyList());
          valueList.addAll(values[i].getValueList());
        }
      }
      sqlParts += ")";
      return new Value(sqlParts, keyList, valueList);
    }

    Value or(Value... values) {
      ArrayList<String> keyList = new ArrayList<>();
      ArrayList<TypedValue> valueList = new ArrayList<>();
      String sqlParts = "(";
      for (int i = 0; i < values.length; i++) {
        String value = values[i].toString();
        if (value != null) {
          sqlParts += (i > 0 ? " or " : "") + value;
          keyList.addAll(values[i].getKeyList());
          valueList.addAll(values[i].getValueList());
        }
      }
      sqlParts += ")";
      return new Value(sqlParts, keyList, valueList);
    }
  }

  public static class Select extends Sql {
    String selectSql = "";
    String whereSql = "";
    String orderSql = "";
    String limitSql = "";

    public Select(Database database, String table, String... keys) {
      super(database);
      selectSql = "select " + listByComma(keys) + " from " + table;
    }

    public Select where(Value value) {
      whereSql = " where " + value.toString() + "";
      valueList.addAll(value.getValueList());
      return this;
    }

    public Select orderBy(String key, boolean desc) {
      if ("".equals(orderSql)) {
        orderSql = " order by ";
       } else {
        orderSql += ",";
      }
      orderSql += key + (desc ? " desc" : "");
      return this;
    }

    public Select orderBy(String key) {
      return orderBy(key, false);
    }

    public Select limit(int limit) {
      limitSql = " limit " + limit;
      return this;
    }

    @Override
    public String toString() {
      return selectSql + whereSql + orderSql + limitSql + ";";
    }
  }

  public static class Update extends Sql {
    String updateSql = "";
    String whereSql = "";

    public Update(Database database, String table, Value... values) {
      super(database);
      ArrayList<String> updateKeys = new ArrayList<>();
      for (Value value : values) {
        updateKeys.add(value.toString());
        valueList.addAll(value.getValueList());
      }
      updateSql = "update " + table + " set " + listByComma(updateKeys.toArray(new String[updateKeys.size()]));
    }

    public Update where(Value value) {
      whereSql = " where " + value.toString() + "";
      valueList.addAll(value.getValueList());
      return this;
    }

    @Override
    public String toString() {
      return updateSql + whereSql + ";";
    }
  }

  public static class Delete extends Sql {
    String deleteSql = "";
    String whereSql = "";

    public Delete(Database database, String table) {
      super(database);
      deleteSql = "delete from " + table;
    }

    public Delete where(Value value) {
      whereSql = " where " + value.toString() + "";
      valueList.addAll(value.getValueList());
      return this;
    }

    @Override
    public String toString() {
      return deleteSql + whereSql + ";";
    }
  }

  public static class Insert extends Sql {
    String sql = "";

    public Insert(Database database, String table, Value... values) {
      super(database);
      ArrayList<String> keyList = new ArrayList<>();
      for (Value value : values) {
        keyList.addAll(value.getKeyList());
        valueList.addAll(value.getValueList());
      }
      sql = "insert into " + table + "(" + listByComma(keyList.toArray(String[]::new)) + ") values(";
      for (int i = 0; i < keyList.size(); i++) {
        String value = keyList.get(i);
        if (value != null) {
          sql += (i > 0 ? ",?" : "?");
        }
      }
      sql += ");";
    }

    @Override
    public String toString() {
      return sql + ";";
    }
  }

  public static class Create extends Sql {
    String sql = "";

    public Create(Database database, String table, String... keys) {
      super(database);
      sql = "create table if not exists " + table + "("  + Sql.listByComma(keys) + ")";
    }

    @Override
    public String toString() {
      return sql + ";";
    }

    public static String withDefault(String key, String defaultValue) {
      return key + " default " + defaultValue;
    }

    public static String timestamp(String key) {
      return key + " timestamp default (DATETIME('now','localtime'))";
    }
  }

  public static class AlterTable extends Sql {
    String sql = "";

    public AlterTable(Database database, String table, String key) {
      super(database);
      sql = "alter table " + table + " add column "  + key + "";
    }

    @Override
    public String toString() {
      return sql + ";";
    }

    public static String withDefault(String key, String defaultValue) {
      return key + " default " + defaultValue;
    }

    public static String timestamp(String key) {
      return key + " timestamp default (DATETIME('now','localtime'))";
    }
  }

  private static String listByComma(String... values) {
    String result = "";
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      if (value != null) {
        result += (result != "" ? ',' : "") + value;
      }
    }
    return result;
  }
}
