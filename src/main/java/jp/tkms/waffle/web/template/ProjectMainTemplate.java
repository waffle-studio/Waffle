package jp.tkms.waffle.web.template;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutablesComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public abstract class ProjectMainTemplate extends MainTemplate {

  private Project project;

  public ProjectMainTemplate(Project project) throws ProjectNotFoundException {
    this.project = project;

    if (project == null) {
      throw new ProjectNotFoundException();
    }
  }

  @Override
  protected ArrayList<Map.Entry<String, String>> pageNavigation() {
    return new ArrayList<Map.Entry<String, String>>(Arrays.asList(
      Map.entry(Html.element("strong", null, project.getName()) + " | Conductors", ProjectComponent.getUrl(project)),
      Map.entry(ExecutablesComponent.EXECUTABLES, ExecutablesComponent.getUrl(project)),
      Map.entry(WorkspaceComponent.WORKSPACES, WorkspaceComponent.getUrl(project))
    ));
  }
}
