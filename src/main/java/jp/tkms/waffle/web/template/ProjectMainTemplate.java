package jp.tkms.waffle.web.template;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutablesComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspacesComponent;

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
    ArrayList<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(Arrays.asList(
      Map.entry(Html.element("strong", null, project.getName()) + " | " + Html.fasIcon("user-tie") + "Conductors", ProjectComponent.getUrl(project)),
      Map.entry( Html.fasIcon("layer-group") + ExecutablesComponent.EXECUTABLES, ExecutablesComponent.getUrl(project)),
      Map.entry(Html.fasIcon("table") + WorkspaceComponent.WORKSPACES, WorkspacesComponent.getUrl(project))
    ));

    Workspace workspace = pageWorkspace();
    if(workspace != null) {
      list.add(Map.entry(Html.element("strong", null, Html.fasIcon("chevron-right") + workspace.getName()) + "", WorkspaceComponent.getUrl(workspace)));
    }

    return list;
  }

  protected Workspace pageWorkspace() {
    return null;
  }
}
