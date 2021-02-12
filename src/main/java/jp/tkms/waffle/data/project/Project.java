package jp.tkms.waffle.data.project;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.ChildElementsArrayList;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.data.util.InstanceCache;
import jp.tkms.waffle.data.web.Data;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public class Project implements DataDirectory, Serializable {
  public static final String PROJECT = "PROJECT";
  private static final InstanceCache<String, Project> instanceCache = new InstanceCache<>();

  protected String name;

  public Project(String name) {
    this.name = name;
    instanceCache.put(name, this);
    initialize();
  }

  public String getName() {
    return name;
  }

  public static Project getInstance(String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath().resolve(name))) {
      Project project = instanceCache.get(name);
      if (project == null) {
        project = new Project(name);
      }
      return project;
    }
    return null;
  }

  public static ArrayList<Project> getList() {
    Data.initializeWorkDirectory();

    return new ChildElementsArrayList().getList(getBaseDirectoryPath(), name -> {
      return getInstance(name.toString());
    });
  }

  public static Project create(String name) {
    Data.initializeWorkDirectory();

    name = FileName.removeRestrictedCharacters(name);

    Project project = getInstance(name);
    if (project == null) {
      project = new Project(name);
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

  public void initialize() {
    Data.initializeWorkDirectory();

    if (! Files.exists(getDirectoryPath())) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(Conductor.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(Conductor.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(Executable.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(Executable.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    if (! Files.exists(Workspace.getBaseDirectoryPath(this))) {
      try {
        Files.createDirectories(Workspace.getBaseDirectoryPath(this));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      Workspace.getTestRunWorkspace(this);
    }
  }
}
