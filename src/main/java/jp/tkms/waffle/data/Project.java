package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class Project extends Data implements DataDirectory {
  protected static final String TABLE_NAME = "project";

  private static final HashMap<String, Project> instanceMap = new HashMap<>();

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

    DataId dataId = DataId.getInstance(id);
    if (dataId != null) {
      Project project = instanceMap.get(dataId.getId());
      if (project != null) {
        project.initialize();
        return project;
      } else {
        project = new Project(dataId.getUuid(), dataId.getPath().getFileName().toString()) ;
        instanceMap.put(project.getId(), project);
        project.initialize();
        return project;
      }
    }

    return null;
  }

  public static Project getInstanceByName(String name) {
    if (name == null) {
      return null;
    }

    DataId dataId = DataId.getInstance(Project.class, getDirectoryPath(name));

    Project project = instanceMap.get(dataId.getId());
    if (project != null) {
      return project;
    }

    project = getInstance(dataId.getId());
    if (project != null) {
      return project;
    }

    if (project == null && Files.exists(getBaseDirectoryPath().resolve(name))) {
      project = create(name);
    }

    return project;
  }

  public static ArrayList<Project> getList() {
    initializeWorkDirectory();

    ArrayList<Project> list = new ArrayList<>();

    /*
    try {
      Files.list(getBaseDirectoryPath()).forEach(path -> {
        if (Files.isDirectory(path)) {
          projectList.add(getInstanceByName(path.getFileName().toString()));
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
     */
    for (File file : getBaseDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        list.add(getInstanceByName(file.getName()));
      }
    }


    return list;
  }

  public static Project create(String name) {
    initializeWorkDirectory();

    Project project = new Project(DataId.getInstance(Project.class, getDirectoryPath(name)).getUuid(), name);
    instanceMap.put(project.getId(), project);

    try {
      Files.createDirectories(project.getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
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

  static public Path getDirectoryPath(String name) {
    return getBaseDirectoryPath().resolve(name);
  }

  @Override
  protected Updater getDatabaseUpdater() {
    return null;
    /*
      new Updater() {
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
     */
  }

  /*
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
   */

  @Override
  public void initialize() {
    super.initialize();
    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(ActorGroup.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(ActorGroup.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(Simulator.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(Simulator.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(RunNode.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(RunNode.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }
}
