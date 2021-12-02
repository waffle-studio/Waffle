package jp.tkms.waffle.web.component.project.workspace.executable;

import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.conductor.ConductorComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutableComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspacesComponent;
import jp.tkms.waffle.web.component.project.workspace.conductor.StagedConductorComponent;
import jp.tkms.waffle.web.template.Html;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class StagedExecutableComponent extends ExecutableComponent {
  public static final String TITLE = "StagedExecutable";
  private Workspace workspace;

  public StagedExecutableComponent(Mode mode) {
    super(mode);
  }

  public StagedExecutableComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new WorkspaceComponent(WorkspaceComponent.Mode.RedirectToWorkspace));
    Spark.get(getUrl(null), new StagedExecutableComponent());
    Spark.post(getUrl(null, Mode.Update), new StagedExecutableComponent(Mode.Update));
    Spark.post(getUrl(null, Mode.UpdateDefaultParameters), new StagedExecutableComponent(Mode.UpdateDefaultParameters));
    Spark.post(getUrl(null, Mode.UpdateDummyResults), new StagedExecutableComponent(Mode.UpdateDummyResults));
    Spark.get(getUrl(null, Mode.TestRun), new StagedExecutableComponent(Mode.TestRun));
    Spark.post(getUrl(null, Mode.TestRun), new StagedExecutableComponent(Mode.TestRun));
  }

  protected static String getUrl() {
    return WorkspaceComponent.getUrl(null) + "/" + Executable.EXECUTABLE;
  }

  public static String getUrl(StagedExecutable executable) {
    if (executable == null) {
      return WorkspaceComponent.getUrl(null) + "/" + Executable.EXECUTABLE + "/:" + KEY_EXECUTABLE;
    } else {
      return WorkspaceComponent.getUrl(executable.getWorkspace()) + "/" + Executable.EXECUTABLE + "/" + executable.getName();
    }
  }

  public static String getUrl(StagedExecutable executable, Mode mode) {
    return getUrl(executable) + "/@" + mode.name();
  }


  @Override
  protected Executable getExecutableEntity() {
    workspace = Workspace.getInstance(project, request.params(WorkspaceComponent.KEY_WORKSPACE));
    return StagedExecutable.getInstance(workspace, request.params(KEY_EXECUTABLE));
  }

  @Override
  protected String renderPageTitle() {
    return TITLE;
  }

  @Override
  protected String renderTool() {
    String value = "";
      /*
    Html.a(getUrl((StagedExecutable) executable, Mode.TestRun),
      Html.span("right badge badge-secondary", null, "TEST RUN")
    );
       */
    Executable parent = Executable.getInstance(project, executable.getName());
    if (parent != null) {
      value += "&nbsp;" + Html.a(ExecutableComponent.getUrl(parent),
        Html.span("right badge badge-light", null, Html.fasIcon("layer-group") + "Base Executable")
      );
    }
    return value;
  }

  @Override
  protected Workspace pageWorkspace() {
    return workspace;
  }

  @Override
  protected ArrayList<String> renderPageBreadcrumb() {
    return new ArrayList<String>(Arrays.asList(
      Html.a(ProjectsComponent.getUrl(), ProjectComponent.PROJECTS),
      Html.a(ProjectComponent.getUrl(project), project.getName()),
      Html.a(WorkspacesComponent.getUrl(project), WorkspaceComponent.WORKSPACES),
      Html.a(WorkspaceComponent.getUrl(workspace), workspace.getName())
    ));
  }
}
