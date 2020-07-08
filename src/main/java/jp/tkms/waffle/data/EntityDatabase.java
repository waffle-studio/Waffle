package jp.tkms.waffle.data;

import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public interface EntityDatabase {
  String KEY_ID = "id";

  Database getDatabase();
  String getEntityTable();
  String getEntityId();


  default String getStringFromDB(String key) {
    String result = null;
    synchronized (getDatabase()) {
      try {
        ResultSet resultSet = new Sql.Select(getDatabase(), getEntityTable(), key).where(Sql.Value.equal(KEY_ID, getEntityId())).executeQuery();
        while (resultSet.next()) {
          result = resultSet.getString(key);
        }
      } catch (SQLException e) {
      }
    }
    return result;
  }

  default Integer getIntFromDB(String key) {
    Integer result = null;
    synchronized (getDatabase()) {
      try {
        ResultSet resultSet = new Sql.Select(getDatabase(), getEntityTable(), key).where(Sql.Value.equal(KEY_ID, getEntityId())).executeQuery();
        while (resultSet.next()) {
          result = resultSet.getInt(key);
        }
      } catch (SQLException e) {
      }
    }
    return result;
  }

  default boolean setToDB(String key, String value) {
    boolean result = false;
    synchronized (getDatabase()) {
      try {
        new Sql.Update(getDatabase(), getEntityTable(), Sql.Value.equal(key, value)).where(Sql.Value.equal(KEY_ID, getEntityId())).execute();
        result = true;
      } catch (SQLException e) {
      }
    }
    return result;
  }

  default boolean setToDB(String key, int value) {
    boolean result = false;
    synchronized (getDatabase()) {
      try {
        new Sql.Update(getDatabase(), getEntityTable(), Sql.Value.equal(key, value)).where(Sql.Value.equal(KEY_ID, getEntityId())).execute();
        result = true;
      } catch (SQLException e) {
      }
    }
    return result;
  }
}
