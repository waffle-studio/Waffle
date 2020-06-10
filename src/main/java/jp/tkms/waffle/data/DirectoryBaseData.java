package jp.tkms.waffle.data;

import jp.tkms.waffle.data.util.Sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public abstract class DirectoryBaseData extends Data implements DataDirectory {
  public static final String KEY_DIRECTORY = "directory";

  private UUID id = null;

  public DirectoryBaseData(String name) {
    this.name = name;
  }

  @Override
  public String getId() {
    if (id == null) {
      final UUID[] id = {null};
      handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          ResultSet resultSet = new Sql.Select(db, getTableName(), KEY_ID).where(Sql.Value.equal(KEY_DIRECTORY, getDirectoryPath().toString())).executeQuery();
          while (resultSet.next()) {
            id[0] = UUID.fromString(resultSet.getString(KEY_ID));
          }
          if (id[0] == null) {
            id[0] = UUID.randomUUID();
            new Sql.Insert(db, getTableName(), Sql.Value.equal(KEY_ID, id[0].toString()), Sql.Value.equal(KEY_DIRECTORY, getDirectoryPath().toString()));
          }
        }
      });
      this.id = id[0];
    }
    return id.toString();
  }

  @Override
  protected String getTableName() {
    return "uuid";
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
      @Override
      String tableName() {
        return getTableName();
      }

      @Override
      ArrayList<UpdateTask> updateTasks() {
        return new ArrayList<Updater.UpdateTask>(Arrays.asList(
          new UpdateTask() {
            @Override
            void task(Database db) throws SQLException {
              new Sql.Create(db, tableName(), KEY_ID, KEY_DIRECTORY).execute();
            }
          }
        ));
      }
    };
  }
}
