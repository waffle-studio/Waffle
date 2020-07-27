package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.util.FileName;

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

public class Project implements DataDirectory {
  protected static final String TABLE_NAME = "project";
  private static final HashMap<String, Project> instanceMap = new HashMap<>();

  protected String name;

  public Project(String name) {
    this.name = name;
    instanceMap.put(this.name, this);
    initialize();
  }

  public String getName() {
    return name;
  }

  public static Project getInstance(String name) {
    if (name != null && !name.equals("") && Files.exists(getBaseDirectoryPath().resolve(name))) {
      Project project = instanceMap.get(name);
      if (project == null) {
        project = new Project(name);
      }
      return project;
    }
    return null;
  }

  public static ArrayList<Project> getList() {
    Data.initializeWorkDirectory();

    ArrayList<Project> list = new ArrayList<>();

    for (File file : getBaseDirectoryPath().toFile().listFiles()) {
      if (file.isDirectory()) {
        list.add(getInstance(file.getName()));
      }
    }

    return list;
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
