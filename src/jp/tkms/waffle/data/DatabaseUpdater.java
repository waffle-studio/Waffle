package jp.tkms.waffle.data;

import java.sql.SQLException;
import java.util.ArrayList;

public abstract class DatabaseUpdater {
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
