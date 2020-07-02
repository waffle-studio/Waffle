package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DataId {
  private static final String FILE_NAME = ".dataid.db";
  private static final String TABLE_NAME = "dataid";
  private static final String KEY_ID = "id";
  private static final String KEY_CLASS = "class";
  private static final String KEY_DIRECTORY = "directory";

  private static final Object objectLocker = new Object();

  private String id;
  private String className;
  private String directory;

  public DataId(String id, String className, String directory) {
    this.id = id;
    this.className = className;
    this.directory = directory;
  }

  public String getId() {
    return id;
  }

  public UUID getUuid() {
    return UUID.fromString(id);
  }

  public String getClassName() {
    return className;
  }

  public String getDirectory() {
    return directory;
  }

  public Path getPath() {
    return Constants.WORK_DIR.resolve(directory);
  }

  /*
  public static DataId getInstance(Path path) {
    DataId dataId = null;
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
    synchronized (objectLocker) {
      Database db = Database.getDatabase(Constants.WORK_DIR.resolve(FILE_NAME));
      try {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_CLASS, KEY_DIRECTORY)
          .where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).executeQuery();
        while (resultSet.next()) {
          dataId = new DataId(
            resultSet.getString(KEY_ID),
            resultSet.getString(KEY_CLASS),
            resultSet.getString(KEY_DIRECTORY)
          );
        }
      } catch (SQLException e) {
        try {
          new Sql.Create(db, TABLE_NAME, KEY_ID, KEY_CLASS, KEY_DIRECTORY).execute();
        } catch (SQLException ex) {
          ErrorLogMessage.issue(ex);
        }
      }
      try {
        db.commit();
        db.close();
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      }
    }
    return dataId;
  }
   */

  public static DataId getInstance(String id) {
    DataId dataId = null;
    synchronized (objectLocker) {
      Database db = Database.getDatabase(Constants.WORK_DIR.resolve(FILE_NAME));
      try {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID, KEY_CLASS, KEY_DIRECTORY)
          .where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          dataId = new DataId(
            resultSet.getString(KEY_ID),
            resultSet.getString(KEY_CLASS),
            resultSet.getString(KEY_DIRECTORY)
          );
        }
      } catch (SQLException e) {
        try {
          new Sql.Create(db, TABLE_NAME, KEY_ID, KEY_CLASS, KEY_DIRECTORY).execute();
        } catch (SQLException ex) {
          ErrorLogMessage.issue(ex);
        }
      }
      try {
        db.commit();
        db.close();
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      }
    }
    return dataId;
  }

  public static DataId getInstance(Class clazz, Path path) {
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
    UUID uuid = UUID.randomUUID();
    synchronized (objectLocker) {
      Database db = Database.getDatabase(Constants.WORK_DIR.resolve(FILE_NAME));
      try {
        ResultSet resultSet = new Sql.Select(db, TABLE_NAME, KEY_ID)
          .where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).executeQuery();
        while (resultSet.next()) {
            uuid = UUID.fromString(resultSet.getString(KEY_ID));
        }
      } catch (SQLException e) {
        try {
          new Sql.Create(db, TABLE_NAME, KEY_ID, KEY_CLASS, KEY_DIRECTORY).execute();
        } catch (SQLException ex) {
          ErrorLogMessage.issue(ex);
        }
      }
      try {
        new Sql.Delete(db, TABLE_NAME).where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).execute();
        new Sql.Insert(db, TABLE_NAME,
          Sql.Value.equal(KEY_ID, uuid.toString()),
          Sql.Value.equal(KEY_CLASS, clazz.getCanonicalName()),
          Sql.Value.equal(KEY_DIRECTORY, localPath.toString())
          ).execute();
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      }
      try {
        db.commit();
        db.close();
      } catch (SQLException e) {
        ErrorLogMessage.issue(e);
      }
    }
    return new DataId(uuid.toString(), clazz.getCanonicalName(), localPath.toString());
  }
}
