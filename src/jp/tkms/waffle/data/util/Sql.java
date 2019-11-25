package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Sql {
  Database database;

  public Sql(Database database) {
    this.database = database;
  }

  public PreparedStatement preparedStatement() throws SQLException {
    return database.preparedStatement(this.toString());
  }

  public static class Value{
    String value = "";

    private Value(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static Value equalP(String key) {
      return new Value(key + "=?");
    }

    public static Value and(Value... values) {
      String sqlParts = "(";
      for (int i = 0; i < values.length; i++) {
        String value = values[i].toString();
        if (value != null) {
          sqlParts += (i > 0 ? " and " : "") + value;
        }
      }
      sqlParts += ")";
      return new Value(sqlParts);
    }

    Value or(Value... values) {
      String sqlParts = "(";
      for (int i = 0; i < values.length; i++) {
        String value = values[i].toString();
        if (value != null) {
          sqlParts += (i > 0 ? " or " : "") + value;
        }
      }
      sqlParts += ")";
      return new Value(sqlParts);
    }
  }

  public static class Select extends Sql {
    String selectSql = "";
    String whereSql = "";

    public Select(Database database, String table, String... keys) {
      super(database);
      selectSql = "select " + listByComma(keys) + " from " + table;
    }

    public Select where(Value value) {
      whereSql = " where " + value.toString() + "";
      return this;
    }

    @Override
    public String toString() {
      return selectSql + whereSql + ";";
    }
  }

  public static class Insert extends Sql {
    String sql = "";

    public Insert(Database database, String table, String... keys) {
      super(database);
      sql = "insert into " + table + "(" + listByComma(keys) + ") values(";
      for (int i = 0; i < keys.length; i++) {
        String value = keys[i];
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
