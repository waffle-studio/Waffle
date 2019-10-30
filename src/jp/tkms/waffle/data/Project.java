package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

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

public class Project extends Data {
  protected static final String TABLE_NAME = "project";

  private Path location;

  public Project(UUID id, String name) {
    super(id, name);
    this.location = Paths.get(Environment.DEFAULT_WD.replaceFirst( "\\$\\{NAME\\}", getUnifiedName()));
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  @Override
  protected DatabaseUpdater getMainDatabaseUpdater() {
    return mainDatabaseUpdater;
  }

  protected void setLocation(Path location) {
    this.location = location;
  }

  public static Project getInstance(String id) {
    Project project = null;
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("select id,name,location from " + TABLE_NAME + " where id=?;");
      statement.setString(1, id);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        project = new Project(
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
        project.setLocation(Paths.get(resultSet.getString("location")));
      }
      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return project;
  }

  public static ArrayList<Project> getList() {
    ArrayList<Project> projectList = new ArrayList<>();
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
      while (resultSet.next()) {
        Project project = new Project(
          UUID.fromString(resultSet.getString("id")),
          resultSet.getString("name")
        );
        project.setLocation(null);
        projectList.add(project);
      }

      db.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return projectList;
  }

  public static Project create(String name) {
    Project project = new Project(UUID.randomUUID(), name);
    try {
      Database db = getMainDB(mainDatabaseUpdater);
      PreparedStatement statement
        = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,location) values(?,?,?);");
      statement.setString(1, project.getId());
      statement.setString(2, project.getName());
      statement.setString(3, project.getLocation().toString());
      statement.execute();
      db.commit();
      db.close();

      Files.createDirectories(project.getLocation());

      ProjectData.getWorkDB(project, new DatabaseUpdater() {
        @Override
        String tableName() {
          return TABLE_NAME;
        }

        @Override
        ArrayList<UpdateTask> updateTasks() {
          return new ArrayList<UpdateTask> (Arrays.asList(
            new UpdateTask() {
              @Override
              void task(Database db) throws SQLException {
                PreparedStatement statement
                  = db.preparedStatement("insert into system(name,value) values('id',?);");
                statement.setString(1, project.getId());
                statement.execute();

                statement = db.preparedStatement("insert into system(name,value) values('name',?);");
                statement.setString(1, project.getName());
                statement.execute();

                db.execute("insert into system(name,value)" +
                  " values('timestamp_create',(DATETIME('now','localtime')));");
              }
            }
          ));
        }
      }).close(); // initialize database
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return project;
  }

  public Path getLocation() {
    return location;
  }

  private static DatabaseUpdater mainDatabaseUpdater = new DatabaseUpdater() {
    @Override
    String tableName() {
      return TABLE_NAME;
    }

    @Override
    ArrayList<DatabaseUpdater.UpdateTask> updateTasks() {
      return new ArrayList<DatabaseUpdater.UpdateTask> (Arrays.asList(
        new UpdateTask() {
          @Override
          void task(Database db) throws SQLException {
            db.execute("create table " + TABLE_NAME + "(id,name,location," +
              "timestamp_create timestamp default (DATETIME('now','localtime'))" +
              ");");
          }
        }
      ));
    }
  };

  private static DatabaseUpdater workDatabaseUpdater = new DatabaseUpdater() {
    @Override
    String tableName() {
      return TABLE_NAME;
    }

    @Override
    ArrayList<DatabaseUpdater.UpdateTask> updateTasks() {
      return new ArrayList<DatabaseUpdater.UpdateTask> (Arrays.asList(
        null // reserved in create()
      ));
    }
  };
}
