package jp.tkms.waffle.web.component.project.workspace;

  import jp.tkms.waffle.Main;
  import jp.tkms.waffle.data.project.workspace.Workspace;
  import jp.tkms.waffle.data.project.workspace.conductor.StagedConductor;
  import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
  import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
  import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
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
  import jp.tkms.waffle.data.project.Project;
  import jp.tkms.waffle.exception.ProjectNotFoundException;
  import spark.Spark;

  import java.util.ArrayList;
  import java.util.Arrays;
  import java.util.Map;
  import java.util.concurrent.Future;

public class WorkspaceComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACES = "Workspaces";
  public static final String WORKSPACE = "Workspaces";
  public static final String KEY_WORKSPACE = "workspace";

  public enum Mode {Default, RedirectToWorkspace}
  private Mode mode;

  private Project project;
  private Workspace workspace;

  public WorkspaceComponent(Mode mode) {
    this.mode = mode;
  }

  public WorkspaceComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new WorkspaceComponent());
    Spark.get(getUrl(null, null), new WorkspaceComponent());

    StagedExecutableComponent.register();
    StagedConductorComponent.register();
    RunComponent.register();
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + Workspace.WORKSPACE;
  }

  public static String getUrl(Project project, Workspace workspace) {
    return ProjectComponent.getUrl(project) + "/" + Workspace.WORKSPACE + "/" + (workspace == null ? ':' + KEY_WORKSPACE : workspace.getName());
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params(ProjectComponent.KEY_PROJECT));
    String workspaceName = request.params(KEY_WORKSPACE);
    if (workspaceName == null) {
      renderWorkspaceList();
    } else {
      workspace = Workspace.getInstance(project, request.params(KEY_WORKSPACE));
      if (mode.equals(Mode.RedirectToWorkspace)) {
        response.redirect(getUrl(project, workspace));
      } else {
        renderWorkspace();
      }
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
              for (Workspace workspace : Workspace.getList(project)) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      Html.a(WorkspaceComponent.getUrl(project, workspace), null, null, workspace.getName()));
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
                      Html.a(WorkspaceComponent.getUrl(project, workspace), null, null, workspace.getName()));
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

  private void renderWorkspace() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return workspace.getName();
      }

      @Override
      protected String pageSubTitle() {
        return WORKSPACE;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(WorkspaceComponent.getUrl(project), WORKSPACES)
        ));
      }

      @Override
      protected Workspace pageWorkspace() {
        return workspace;
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
        return
          Lte.divContainerFluid(Lte.divRow(
            Html.section("col-lg-6",
            Lte.card(Html.fasIcon("user-tie") + "Staged" + ConductorComponent.CONDUCTORS, null,
              Lte.table("table-condensed", new Lte.Table() {
                @Override
                public ArrayList<Lte.TableValue> tableHeaders() {
                  return null;
                }

                @Override
                public ArrayList<Future<Lte.TableRow>> tableRows() {
                  ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                  for (StagedConductor conductor : StagedConductor.getList(workspace)) {
                    list.add(Main.interfaceThreadPool.submit(() -> {
                        return new Lte.TableRow(
                          Html.a(StagedConductorComponent.getUrl(conductor), null, null, conductor.getName()));
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
              , null, "card-warning card-outline", "p-0")
              , Lte.card(Html.fasIcon("layer-group") + "Staged" + ExecutablesComponent.EXECUTABLES, null,
                Lte.table("table-condensed", new Lte.Table() {
                  @Override
                  public ArrayList<Lte.TableValue> tableHeaders() {
                    return null;
                  }

                  @Override
                  public ArrayList<Future<Lte.TableRow>> tableRows() {
                    ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                    for (StagedExecutable executable : StagedExecutable.getList(workspace)) {
                      list.add(Main.interfaceThreadPool.submit(() -> {
                          return new Lte.TableRow(
                            Html.a(StagedExecutableComponent.getUrl(executable), null, null, executable.getName()));
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
                , null, "card-info card-outline", "p-0")
            ),
            Html.section("col-lg-6",
              Lte.card(Html.fasIcon("project-diagram") + RunComponent.RUNS, null,
                Lte.table("table-condensed", new Lte.Table() {
                  @Override
                  public ArrayList<Lte.TableValue> tableHeaders() {
                    return null;
                  }

                  @Override
                  public ArrayList<Future<Lte.TableRow>> tableRows() {
                    ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                    for (AbstractRun abstractRun : AbstractRun.getList(workspace)) {
                      list.add(Main.interfaceThreadPool.submit(() -> {
                          if (abstractRun instanceof ExecutableRun) {
                            return new Lte.TableRow(
                              Html.a(RunComponent.getUrl(abstractRun), null, null, abstractRun.getName()));
                          } else {
                            return new Lte.TableRow(
                              Html.a(RunComponent.getUrl(abstractRun), null, null, abstractRun.getName()));
                          }
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
            )
          ));
      }
    }.render(this);
  }
}
