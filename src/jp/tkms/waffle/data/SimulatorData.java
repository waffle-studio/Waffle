package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.io.File;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

abstract public class SimulatorData extends Data {
  private Project project;

  public SimulatorData(Project project, UUID id, String name) {
    super(id, name);
    this.project = project;
  }

  abstract protected String getTableName();

  abstract protected Updater getWorkUpdater();

  public boolean isValid() {
    return id != null;
  }

  public Project getProject() {
    return project;
  }

  protected String getFromDB(String key) {
    final String[] result = {null};

    handleWorkDB(project, getWorkUpdater(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
        statement.setString(1, getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          result[0] = resultSet.getString(key);
        }
      }
    });

    return result[0];
  }

  public void setName(String name) {
    handleWorkDB(project, getWorkUpdater(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("update " + getTableName() + " set " + KEY_NAME + "=? where " + KEY_ID + "=?");
        statement.setString(1, name);
        statement.setString(2, getId());
        statement.execute();
      }
    });
  }

  private static Database getWorkDB(Project project) {
    return Database.getDatabase(Paths.get(project.getLocation() + File.separator + Environment.WORK_DB_NAME));
  }

  private static Database getWorkDB(Project project, Updater updater) {
    Database db = getWorkDB(project);
    if (updater != null) {
      updater.update(db);
    }
    return db;
  }

  synchronized protected static boolean handleWorkDB(Project project, Updater updater, Handler handler) {
    boolean isSuccess = true;

    try(Database db = getWorkDB(project, updater)) {
      handler.handling(db);
      db.commit();
    } catch (SQLException e) {
      isSuccess = false;
      e.printStackTrace();
    }

    return isSuccess;
  }
}
