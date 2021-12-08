package jp.tkms.waffle.web.component.project.workspace;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.HasLocalPath;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.conductor.StagedConductor;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.conductor.ConductorComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutablesComponent;
import jp.tkms.waffle.web.component.project.workspace.conductor.StagedConductorComponent;
import jp.tkms.waffle.web.component.project.workspace.executable.StagedExecutableComponent;
import jp.tkms.waffle.web.component.project.workspace.run.RunComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

public class WorkspacesComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACES = "Workspaces";

  private Project project;

  public WorkspacesComponent() {
  }

  static public void register() {
    Spark.get(getUrl(null), new WorkspacesComponent());

    WorkspaceComponent.register();
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + Workspace.WORKSPACE;
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params(ProjectComponent.KEY_PROJECT));
    renderWorkspaceList();
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
          Lte.table("table-condensed table-sm table-nooverflow", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("", "Note"));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Workspace workspace : Workspace.getList(project)) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      Html.a(WorkspaceComponent.getUrl(workspace), null, null, workspace.getName()),
                      workspace.getNote()
                    );
                  }
                ));
              }
              if (list.isEmpty()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(new Lte.TableValue("text-align:center;color:silver;", Html.fasIcon("receipt") + "Empty"));
                  }
                ));
              }
              return list;
            }
          })
          , null, "card-danger card-outline", "p-0")
        + Lte.card( Html.fasIcon("eye-slash") + "Hidden Workspaces", Lte.cardToggleButton(true),
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("", "Name"));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Workspace workspace : Workspace.getHiddenList(project)) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      Html.a(WorkspaceComponent.getUrl(workspace), null, null, workspace.getName())
                      );
                  }
                ));
              }
              if (list.isEmpty()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(new Lte.TableValue("text-align:center;color:silver;", Html.fasIcon("receipt") + "Empty"));
                  }
                ));
              }
              return list;
            }
          })
          , null, "collapsed-card card-secondary card-outline", "p-0");
      }
    }.render(this);
  }

}