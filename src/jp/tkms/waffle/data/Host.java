package jp.tkms.waffle.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Host extends Data {
  private static final String TABLE_NAME = "host";
  private static final UUID LOCAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final String KEY_WORKBASE = "workbase";
  private static final String KEY_POLLING = "polling";

  private String simulationCommand = null;
  private String versionCommand = null;

  public Host(UUID id, String name) {
    super(id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Host getInstance(String id) {
    Host host = null;
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        host = new Host(
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return host;
  }

  public static ArrayList<Host> getList() {
    ArrayList<Host> simulatorList = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        simulatorList.add(new Host(
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name"))
        );
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return simulatorList;
  }

  public static Host create(String name, String simulationCommand, String versionCommand) {
    return null;
  }

  public Path getLocation() {
    Path path = Paths.get( TABLE_NAME + File.separator + name + '_' + shortId );
    return path;
  }

  public boolean isLocal() {
    return LOCAL_UUID.equals(id);
  }

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return mainDatabaseUpdater;
  }

  private static DatabaseUpdater mainDatabaseUpdater = new DatabaseUpdater() {
      @Override
      String tableName() {
        return TABLE_NAME;
      }

      @Override
      ArrayList<DatabaseUpdater.UpdateTask> updateTasks() {
        return new ArrayList<DatabaseUpdater.UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              db.execute("create table " + TABLE_NAME + "(id,name," +
                KEY_WORKBASE + "," +
                KEY_POLLING + "," +
                ");");
              db.execute("insert into " + TABLE_NAME
                + "(id,name) values('" + LOCAL_UUID.toString() + "','LOCAL');");
            }
          }
        ));
      }
    };
}
