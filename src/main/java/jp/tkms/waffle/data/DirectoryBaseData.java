package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public abstract class DirectoryBaseData extends Data implements DataDirectory {

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
      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          ResultSet resultSet = new Sql.Select(db, KEY_UUID, KEY_ID).where(Sql.Value.equal(KEY_DIRECTORY, getDirectoryPath().toString())).executeQuery();
          while (resultSet.next()) {
            id[0] = UUID.fromString(resultSet.getString(KEY_ID));
          }
          if (id[0] == null) {
            id[0] = UUID.randomUUID();
            new Sql.Insert(db, KEY_UUID, Sql.Value.equal(KEY_ID, id[0].toString()), Sql.Value.equal(KEY_DIRECTORY, getDirectoryPath().toString())).execute();
          }
        }
      });
      this.id = id[0];
    }
    return this.id;
  }
}
