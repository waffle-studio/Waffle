package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.conductor.TestConductor;

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

  public Project() { }

  protected void setLocation(Path location) {
    this.location = location;
  }

  public static Project getInstance(String id) {
    final Project[] project = {null};

    handleDatabase(new Project(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement
          = db.preparedStatement("select id,name,location from " + TABLE_NAME + " where id=?;");
        statement.setString(1, id);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          project[0] = new Project(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
          project[0].setLocation(Paths.get(resultSet.getString("location")));
        }
      }
    });

    return project[0];
  }

  public static ArrayList<Project> getList() {
    ArrayList<Project> projectList = new ArrayList<>();

    handleDatabase(new Project(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          Project project = new Project(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
          project.setLocation(null);
          projectList.add(project);
        }
      }
    });

    return projectList;
  }

  public static Project create(String name) {
    Project project = new Project(UUID.randomUUID(), name);

    if (
      handleDatabase(project, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,location) values(?,?,?);");
          statement.setString(1, project.getId());
          statement.setString(2, project.getName());
          statement.setString(3, project.getLocation().toString());
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(project.getLocation());

        if (new ProjectInitializer(project).init()) {
          Conductor.create(project, "Trial Submitter",
            AbstractConductor.getInstance(TestConductor.class.getCanonicalName()), "");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    return project;
  }

  public Path getLocation() {
    return location;
  }


  @Override
  protected Updater getDatabaseUpdater() {
    return new Updater() {
      @Override
      String tableName() {
        return TABLE_NAME;
      }

      @Override
      ArrayList<Updater.UpdateTask> updateTasks() {
        return new ArrayList<Updater.UpdateTask>(Arrays.asList(
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
  }

  private static class ProjectInitializer extends ProjectData {
    public ProjectInitializer(Project project) {
      super(project);
    }

    boolean init() {
      return handleDatabase(this, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into system(name,value) values('id',?);");
          statement.setString(1, getProject().getId());
          statement.execute();

          statement = db.preparedStatement("insert into system(name,value) values('name',?);");
          statement.setString(1, getProject().getName());
          statement.execute();

          db.execute("insert into system(name,value)" +
            " values('timestamp_create',(DATETIME('now','localtime')));");
        }
      });
    }

    @Override
    protected String getTableName() {
      return null;
    }

    @Override
    protected Updater getDatabaseUpdater() { return null; }
  }
}
