package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

abstract public class Data {
  protected static final String KEY_ID = "id";
  protected static final String KEY_NAME = "name";

  protected UUID id = null;
  protected String shortId;
  protected String name;

  public Data() {}

  public Data(UUID id, String name) {
    this.id = id;
    this.shortId = getShortId(id);
    this.name = name;
  }

  abstract protected String getTableName();

  abstract protected Updater getDatabaseUpdater();

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public static String getShortName(String name) {
    String replacedName = name.replaceAll("[^0-9a-zA-Z_\\-]", "");
    return replacedName.substring(0, (replacedName.length() < 8 ? replacedName.length() : 8));
  }

  public static String getUnifiedName(UUID id, String name) {
    return getShortName(name) + '_' + getShortId(id);
  }

  public String getUnifiedName() {
    return getShortName() + '_' + getShortId();
  }

  public boolean isValid() {
    return id != null;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id.toString();
  }

  public String getShortId() {
    return shortId;
  }

  public String getShortName() {
    return getShortName(name);
  }

  protected void setSystemValue(String name, Object value) {
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Insert(db, Database.SYSTEM_TABLE, Database.KEY_NAME, Database.KEY_VALUE).toPreparedStatement();
        statement.setString(1, name);
        statement.setString(2, value.toString());
        statement.execute();
      }
    });
  }

  protected String getFromDB(String key) {
    final String[] result = {null};

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          result[0] = resultSet.getString(key);
        }
      }
    });

    return result[0];
  }

  protected boolean setStringToDB(String key, String value) {
    return handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), key).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setString(1, value);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  protected boolean setIntToDB(String key, int value) {
    return handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = new Sql.Update(db, getTableName(), key).where(Sql.Value.equalP(KEY_ID)).toPreparedStatement();
        statement.setInt(1, value);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  public void setName(String name) {
    if (
      setStringToDB(KEY_NAME, name)
    ) {
      this.name = name;
    }
  }

  protected Database getDatabase() {
    return Database.getDatabase(null);
  }

  synchronized protected static boolean handleDatabase(Data base, Handler handler) {
    boolean isSuccess = true;

    try(Database db = base.getDatabase()) {
      Updater updater = base.getDatabaseUpdater();
      if (updater != null) {
        updater.update(db);
      } else {
        db.getVersion(null);
      }
      handler.handling(db);
      db.commit();
    } catch (SQLException e) {
      isSuccess = false;
      e.printStackTrace();
    }

    return isSuccess;
  }

  abstract static class Handler {
    abstract void handling(Database db) throws SQLException;
  }

  public abstract static class Updater {
    public abstract class UpdateTask {
      abstract void task(Database db) throws SQLException;
    }

    abstract String tableName();

    abstract ArrayList<UpdateTask> updateTasks();

    void update(Database db) {
      try {
        int version = db.getVersion(tableName());

        for (; version < updateTasks().size(); version++) {
          updateTasks().get(version).task(db);
        }

        db.setVersion(tableName(), version);
        db.commit();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
