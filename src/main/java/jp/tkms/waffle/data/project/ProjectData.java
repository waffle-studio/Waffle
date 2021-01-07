package jp.tkms.waffle.data.project;

abstract public class ProjectData {
  private Project project;

  public ProjectData(Project project) {
    this.project = project;
  }

  public Project getProject() {
    return project;
  }
}
