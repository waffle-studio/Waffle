package jp.tkms.waffle.data;

import jp.tkms.waffle.Environment;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

abstract public class ProjectData extends Data {
  private Project project;

  public ProjectData(Project project, UUID id, String name) {
    super(id, name);
    this.project = project;
  }

  public ProjectData(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }

  protected Database getDatabase() {
    return Database.getDatabase(Paths.get(project.getLocation() + File.separator + Environment.PROJECT_DB_NAME));
  }
}
