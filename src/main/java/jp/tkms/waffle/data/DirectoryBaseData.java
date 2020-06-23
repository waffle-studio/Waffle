package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.util.Sql;

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

  public DirectoryBaseData(String name) {
    this.name = name;
  }

  @Override
  protected String getTableName() {
    return null;
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return null;
  }

  @Override
  public UUID getUuid() {
    if (id == null) {
      final UUID[] id = {null};
      final String className = this.getClass().getCanonicalName();
      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          Path localPath = Constants.WORK_DIR.relativize(getDirectoryPath().toAbsolutePath());
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
    return this.id;
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

  static Path getDirectory(String id) {
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
