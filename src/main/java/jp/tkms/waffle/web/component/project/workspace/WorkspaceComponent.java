package jp.tkms.waffle.web.component.project.workspace;

  import jp.tkms.waffle.Main;
  import jp.tkms.waffle.data.project.workspace.Workspace;
  import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
  import jp.tkms.waffle.web.component.project.ProjectComponent;
  import jp.tkms.waffle.web.component.project.ProjectsComponent;
  import jp.tkms.waffle.web.template.Html;
  import jp.tkms.waffle.web.template.Lte;
  import jp.tkms.waffle.web.template.ProjectMainTemplate;
  import jp.tkms.waffle.data.project.Project;
  import jp.tkms.waffle.exception.ProjectNotFoundException;
  import spark.Spark;

  import java.util.ArrayList;
  import java.util.Arrays;
  import java.util.concurrent.Future;

public class WorkspaceComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACES = "Workspaces";

  private Project project;
  private Workspace workspace;

  public WorkspaceComponent() {
  }

  static public void register() {
    Spark.get(getUrl(null), new WorkspaceComponent());
    Spark.get(getUrl(null, null), new WorkspaceComponent());
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + Workspace.WORKSPACE;
  }

  public static String getUrl(Project project, Workspace workspace) {
    return ProjectComponent.getUrl(project) + "/" + Workspace.WORKSPACE + "/" + (workspace == null ? ":name" : workspace.getName());
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    String name = request.params("name");

    if (name == null) {
      renderWorkspaceList();
    } else {
      workspace = Workspace.find(project, name);
    }
  }

  private void renderWorkspaceList() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return WORKSPACES;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName())));
      }

      @Override
      protected String pageContent() {
        ArrayList<Workspace> workspaceList = Workspace.getList(project);
        /*
        if (executableList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl(project, jp.tkms.waffle.web.component.project.executable.ExecutablesComponent.Mode.New), null, null,
              Html.fasIcon("plus-square") + "Add Executable"
            ),
            null
          );
        }
         */
        return Lte.card(null, null,
          Lte.table("table-condensed", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("", "Name"));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Workspace workspace : workspaceList) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      Html.a(WorkspaceComponent.getUrl(project, workspace), null, null, workspace.getName()));
                  }
                ));
              }
              return list;
            }
          })
          , null, null, "p-0");
      }
    }.render(this);
  }

  public enum Mode {Default}
}
