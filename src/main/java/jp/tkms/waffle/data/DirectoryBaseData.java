package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.Sql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public abstract class DirectoryBaseData extends Data implements DataDirectory {

  private int rowid;

  public DirectoryBaseData() {}

  public DirectoryBaseData(Path path) {
    this.name = path.getFileName().toString();

    final UUID[] id = {null};
    String className = this.getClass().getCanonicalName();
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, KEY_UUID, KEY_ID).where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).executeQuery();
        while (resultSet.next()) {
          id[0] = UUID.fromString(resultSet.getString(KEY_ID));
        }
        if (id[0] == null) {
          id[0] = UUID.randomUUID();
          new Sql.Insert(db, KEY_UUID,
            Sql.Value.equal(KEY_ID, id[0].toString()),
            Sql.Value.equal(KEY_NAME, name),
            Sql.Value.equal(KEY_CLASS, className),
            Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).execute();
        }
      }
    });
    this.id = id[0];
  }

  @Override
  protected String getTableName() {
    return null;
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return null;
  }

  public void replace(Path path) {
    int count = 0;
    while (Files.exists(path)) {
      path = path.getParent().resolve(path.getFileName().toString() + '_' + count++);
      //name = (name.length() > 0 ? "_" : "") + UUID.randomUUID().toString().replaceFirst("-.*$", "");
    }
    Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
    String id = getId();

    if (Files.exists(getDirectoryPath())) {
      try {
        Files.move(getDirectoryPath(), path);
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    handleDatabase(this, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        new Sql.Delete(db, KEY_UUID).where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).execute();
        new Sql.Update(db, KEY_UUID,
          Sql.Value.equal(KEY_DIRECTORY, localPath.toString())
          ).where(Sql.Value.equal(KEY_ID, id)).execute();
      }
    });
  }

  static public void resetUuid(Path path) {
    handleDatabase(null, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        Path localPath = Constants.WORK_DIR.relativize(path.toAbsolutePath());
        new Sql.Delete(db, KEY_UUID).where(Sql.Value.equal(KEY_DIRECTORY, localPath.toString())).execute();
      }
    });
  }

  static String getName(String id) {
    final String[] name = {null};

    handleDatabase(null, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, KEY_UUID, KEY_NAME).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          name[0] = resultSet.getString(KEY_NAME);
        }
      }
    });

    return name[0];
  }

  static Path getDirectoryPath(String id) {
    final Path[] path = {null};

    handleDatabase(null, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = new Sql.Select(db, KEY_UUID, KEY_DIRECTORY).where(Sql.Value.equal(KEY_ID, id)).executeQuery();
        while (resultSet.next()) {
          path[0] = Constants.WORK_DIR.resolve(resultSet.getString(KEY_DIRECTORY));
        }
      }
    });

    return path[0];
  }
}
