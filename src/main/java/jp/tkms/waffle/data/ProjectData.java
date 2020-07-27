package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

abstract public class ProjectData {
  private Project project;

  public ProjectData(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }
}
