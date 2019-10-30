package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.io.File;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

abstract public class ProjectData extends Data {
  protected Project project;

  public ProjectData(Project project, UUID id, String name) {
    super(id, name);
    this.project = project;
  }

  abstract protected String getTableName();

  abstract protected DatabaseUpdater getWorkDatabaseUpdater();

  public static String getShortId(UUID id) {
    return id.toString().replaceFirst("-.*$", "");
  }

  public static String getUnifiedName(UUID id, String name) {
    return name + '_' + getShortId(id);
  }

  public String getUnifiedName() {
    return name + '_' + getShortId(id);
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

  public Project getProject() {
    return project;
  }

  protected String getFromDB(String key) {
    String result = null;
    try {
      Database db = getWorkDB();
      PreparedStatement statement = db.preparedStatement("select " + key + " from " + getTableName() + " where id=?;");
      statement.setString(1, getId());
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        result = resultSet.getString(key);
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static Database getWorkDB(Project project) {
    return Database.getDB(Paths.get(project.getLocation() + File.separator + Environment.WORK_DB_NAME));
  }

  protected static Database getWorkDB(Project project, DatabaseUpdater updater) {
    Database db = getWorkDB(project);
    if (updater != null) {
      updater.update(db);
    }
    return db;
  }

  protected Database getWorkDB() {
    return getWorkDB(project, getWorkDatabaseUpdater());
  }
}
