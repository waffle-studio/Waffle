package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.AbstractConductor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Project extends Data implements DataDirectory {
  protected static final String TABLE_NAME = "project";

  public Project(UUID id, String name) {
    super(id, name);
    initialize();
  }

  @Override
  protected String getTableName() {
    return TABLE_NAME;
  }

  public Project() { }

  public static Project getInstance(String id) {
    initializeWorkDirectory();

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
        }
      }
    });

    project[0].initialize();

    return project[0];
  }

  public static Project getInstanceByName(String name) {
    final Project[] project = {null};

    handleDatabase(new Project(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        PreparedStatement statement = db.preparedStatement("select id,name from " + TABLE_NAME + " where name=?;");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          project[0] = new Project(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
        }
      }
    });

    if (project[0] == null && Files.exists(getBaseDirectoryPath().resolve(name))) {
      project[0] = create(name);
    }

    return project[0];
  }

  public static ArrayList<Project> getList() {
    initializeWorkDirectory();

    ArrayList<Project> projectList = new ArrayList<>();

    try {
      Files.list(getBaseDirectoryPath()).forEach(path -> {
        if (Files.isDirectory(path)) {
          projectList.add(getInstanceByName(path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    /*
    handleDatabase(new Project(), new Handler() {
      @Override
      void handling(Database db) throws SQLException {
        ResultSet resultSet = db.executeQuery("select id,name from " + TABLE_NAME + ";");
        while (resultSet.next()) {
          Project project = new Project(
            UUID.fromString(resultSet.getString("id")),
            resultSet.getString("name")
          );
          projectList.add(project);
        }
      }
    });
     */

    return projectList;
  }

  public static Project create(String name) {
    initializeWorkDirectory();

    Project project = new Project(UUID.randomUUID(), name);

    if (
      handleDatabase(project, new Handler() {
        @Override
        void handling(Database db) throws SQLException {
          PreparedStatement statement
            = db.preparedStatement("insert into " + TABLE_NAME + "(id,name,location) values(?,?,?);");
          statement.setString(1, project.getId());
          statement.setString(2, project.getName());
          statement.setString(3, project.getDirectoryPath().toString());
          statement.execute();
        }
      })
    ) {
      try {
        Files.createDirectories(project.getDirectoryPath());

        if (new ProjectInitializer(project).init()) {
          /*
          Conductor.create(project, "Trial Submitter",
            AbstractConductor.getInstance(TestConductor.class.getCanonicalName()), "");
           */
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    return project;
  }

  public static Path getBaseDirectoryPath() {
    return Data.getWaffleDirectoryPath().resolve(Constants.PROJECT);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath().resolve(name);
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

  @Override
  public void initialize() {
    super.initialize();
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (! Files.exists(ActorGroup.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(ActorGroup.getBaseDirectoryPath(this));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (! Files.exists(Simulator.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(Simulator.getBaseDirectoryPath(this));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (! Files.exists(RunNode.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(RunNode.getBaseDirectoryPath(this));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void hibernate() {
    for (Actor entity : Actor.getNotFinishedList(this)) {
      AbstractConductor.getInstance(entity).hibernate(entity);
    }
  }
}
