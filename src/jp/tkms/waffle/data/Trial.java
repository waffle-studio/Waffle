package jp.tkms.waffle.data;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Trial extends ProjectData {
  protected static final String TABLE_NAME = "trials";
  public static final String ROOT_NAME = "ROOT";
  private static final String KEY_PARENT = "parent";

  public Trial(Project project, UUID id, String name) {
    super(project, id, name);
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public static Trial getInstance(Project project, String id) {
    final Trial[] trials = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          trials[0] = new Trial(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return trials[0];
  }

  public static Trial getRootInstance(Project project) {
    final Trial[] trials = {null};

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet
          = db.executeQuery("select id,name from " + TABLE_NAME + " where name='" + ROOT_NAME + "';");
        while (resultSet.next()) {
          trials[0] = new Trial(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    return trials[0];
  }

  public static ArrayList<Trial> getList(Project project, Trial parent) {
    ArrayList<Trial> list = new ArrayList<>();

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select id,name from " + TABLE_NAME + " where " + KEY_PARENT + "=?;");
        statement.setString(1, parent.getId());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          list.add(new Trial(
            project,
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name"))
          );
        }
      }
    });

    return list;
  }

  public static Trial create(Project project, Trial parent, String name) {
    Trial trial = new Trial(project, UUID.randomUUID(), name);

    handleWorkDB(project, workUpdater, new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("insert into " + TABLE_NAME + "(id,name," + KEY_PARENT + ") values(?,?,?);");
        statement.setString(1, trial.getId());
        statement.setString(2, trial.getName());
        statement.setString(3, parent.getId());
        statement.execute();
      }
    });

    return trial;
  }

  public Trial getParent() {
    String parentId = getFromDB(KEY_PARENT);
    return getInstance(getProject(), parentId);
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  public ArrayList<Trial> getChildTrialList() {
    return getList(getProject(), this);
  }

  public ArrayList<Run> getChildRunList() {
    return Run.getList(getProject(), this);
  }

  public Path getLocation() {
    Path path = Paths.get(getProject().getLocation().toAbsolutePath() + File.separator +
      TABLE_NAME + File.separator + name + '_' + shortId
    );
    return path;
  }

  public boolean isRunning() {
    for (Run run : getChildRunList()) {
      if (run.isRunning()) {
        return true;
      }
    }

    for (Trial trial : getChildTrialList()) {
      if (trial.isRunning()) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected Updater getMainUpdater() {
    return null;
  }

  @Override
  protected Updater getWorkUpdater() {
    return workUpdater;
  }

  private static Updater workUpdater = new Updater() {
    @Override
    String tableName() {
      return TABLE_NAME;
    }

    @Override
    ArrayList<UpdateTask> updateTasks() {
      return new ArrayList<UpdateTask>(Arrays.asList(
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            db.execute("create table " + TABLE_NAME + "(" +
              "id,name," + KEY_PARENT + "," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        },
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            String scriptName = "test";
            PreparedStatement statement = db.preparedStatement("insert into " + TABLE_NAME + "(" +
              "id,name," +
              KEY_PARENT +
              ") values(?,?,?);");
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, ROOT_NAME);
            statement.setString(3, "");
            statement.execute();
          }
        }
      ));
    }
  };
}
