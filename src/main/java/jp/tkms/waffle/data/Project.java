package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Project extends PropertyFileData implements DataDirectory {
  protected static final String TABLE_NAME = "project";

  private static final HashMap<String, Project> instanceMap = new HashMap<>();

  public Project(UUID id, String name) {
    super(id, name);
    initialize();
  }

  public Project() { }

  /*
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
   */

  public static Project getInstanceByName(String name) {
    if (name == null) {
      return null;
    }

    //DataId dataId = DataId.getInstance(Project.class, getDirectoryPath(name));

    Project project = instanceMap.get(name);
    if (project != null) {
      project.initialize();
      return project;
    }

    if (project == null && Files.exists(getBaseDirectoryPath().resolve(name))) {
      project = create(name);
      project.initialize();
      return project;
    }

    return project;
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


    return projectList;
  }

  public static Project create(String name) {
    initializeWorkDirectory();

    Project project = new Project(UUID.randomUUID(), name);
    instanceMap.put(project.getId(), project);

    try {
      Files.createDirectories(project.getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    return project;
  }

  public static Path getBaseDirectoryPath() {
    return PropertyFileData.getWaffleDirectoryPath().resolve(Constants.PROJECT);
  }

  @Override
  public Path getDirectoryPath() {
    return getBaseDirectoryPath().resolve(name);
  }

  static public Path getDirectoryPath(String name) {
    return getBaseDirectoryPath().resolve(name);
  }

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
