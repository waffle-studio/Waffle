package jp.tkms.waffle.web.component.project;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.web.component.*;
import jp.tkms.waffle.web.component.project.conductor.ConductorComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutableComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutablesComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

import static jp.tkms.waffle.web.template.Html.*;

public class ProjectComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Project";
  public static final String PROJECTS = "Projects";
  public static final String KEY_PROJECT = "project";

  public enum Mode {Default, NotFound, EditConstModel, AddConductor}
  Mode mode;

  private String requestedId;
  private Project project;
  public ProjectComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ProjectComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ProjectComponent());
    Spark.get(getUrl(null, Mode.EditConstModel), new ProjectComponent());
    Spark.get(getUrl(null, Mode.AddConductor), new ProjectComponent(Mode.AddConductor));
    Spark.post(getUrl(null, Mode.AddConductor), new ProjectComponent(Mode.AddConductor));

    ExecutablesComponent.register();
    ExecutableComponent.register();
    ConductorComponent.register();
    WorkspaceComponent.register();
  }

  public static String getUrl(Project project) {
    return "/" + Project.PROJECT + "/" + (project == null ? ':' + KEY_PROJECT : project.getName());
  }

  public static String getUrl(Project project, Mode mode) {
    return getUrl(project) + "/@" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    Mode mode = this.mode;

    requestedId = request.params(KEY_PROJECT);
    project = Project.getInstance(requestedId);

    if (project == null) {
      mode = Mode.NotFound;
    }

    switch (mode) {
      case EditConstModel:
      case Default:
        renderProject();
        break;
      case AddConductor:
        if (request.requestMethod().toLowerCase().equals("post")) {
          ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
          if (errors.isEmpty()) {
            addConductor();
          } else {
            renderConductorAddForm(errors);
          }
        }
        renderConductorAddForm(new ArrayList<>());
        break;
      case NotFound:
        renderProjectNotFound();
        break;
    }
  }

  private void renderProjectNotFound() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "[" + requestedId + "]";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"), "NotFound"));
      }

      @Override
      protected String pageContent() {
        ArrayList<Project> projectList = Project.getList();
        return Lte.card(null, null,
          Html.h1("text-center", Html.fasIcon("question")),
          null
        );
      }
    }.render(this);
  }

  private void renderConductorAddForm(ArrayList<Lte.FormError> errors) throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Conductor";
      }

      @Override
      protected String pageSubTitle() {
        return "(new)";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "Conductors"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(project, Mode.AddConductor), Html.Method.Post,
            Lte.card("New Conductor", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private void renderProject() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return project.getName();
      }

      @Override
      protected String pageSubTitle() {
        return TITLE;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects")));
      }

      @Override
      protected String pageContent() {
        String content = Html.javascript("sessionStorage.setItem('latest-project-id','" + project.getName() + "');sessionStorage.setItem('latest-project-name','" + project.getName() + "');");
        content += Lte.divRow(
          //Lte.infoBox(Lte.DivSize.F12Md12Sm6, "project-diagram", "bg-danger",
          //  Html.a(RunsComponent.getUrl(project), "Runs"), ""),
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "table", "bg-danger",
            Html.a(WorkspaceComponent.getUrl(project), WorkspaceComponent.WORKSPACES), ""),
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "layer-group", "bg-info",
            Html.a(ExecutablesComponent.getUrl(project), ExecutablesComponent.EXECUTABLES), "")
        );

        ArrayList<Conductor> conductorList = Conductor.getList(project);
        if (conductorList.size() <= 0) {
          content += Lte.card(Html.fasIcon("user-tie") + "Conductors",
            null,
            Html.a(getUrl(project, Mode.AddConductor), null, null,
              Html.fasIcon("plus-square") + "Add Conductors"
            ),
            null, "card-warning card-outline", null
          );
        } else {
          //ArrayList<ActorRun> notFinishedList = new ArrayList<>();
          /*
          for (Actor notFinished : Actor.getNotFinishedList(project)) {
            if (!notFinished.isRoot()) {
              if (notFinished.getParentActor() != null && notFinished.getParentActor().isRoot()) {
                notFinishedList.add(notFinished);
              }
            }
          }

          content += Html.element("script", new Attributes(value("type", "text/javascript")),
              "var updateConductorJobNum = function(c,n) {" +
              "if (n > 0) {" +
              "document.getElementById('conductor-jobnum-' + c).style.display = 'inline-block';" +
              "document.getElementById('conductor-jobnum-' + c).innerHTML = n;" +
              "} else {" +
              "document.getElementById('conductor-jobnum-' + c).style.display = 'none';" +
              "}" +
              "};"
          );
           */

          content += Lte.card(Html.fasIcon("user-tie") + ConductorComponent.CONDUCTORS,
            Html.a(getUrl(project, Mode.AddConductor),
              null, null, Lte.badge("primary", null, Html.fasIcon("plus-square") + "NEW")
            ),
            Lte.table(null, new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                return null;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                for (Conductor conductor : Conductor.getList(project)) {
                  int runningCount = 0;
                  /*
                  for (Actor notFinished : notFinishedList) {
                    if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getId().equals(conductor.getId())) {
                      runningCount += 1;
                    }
                  }
                   */

                  int finalRunningCount = runningCount;
                  list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      new Lte.TableValue("",
                        Html.a(ConductorComponent.getUrl(conductor),
                          null, null, conductor.getName()
                        )),
                      new Lte.TableValue("text-align:right;",
                        Html.a(ConductorComponent.getUrl(conductor, ConductorComponent.Mode.Prepare),
                          Html.span("right badge badge-secondary", null, "RUN")
                        )
                      /*,
                      new Lte.TableValue("text-align:right;",
                        Html.span(null, null,
                          Html.span("right badge badge-warning", new Html.Attributes(value("id", "conductor-jobnum-" + conductor.getLocalDirectoryPath().toString())))
                          ,
                          "gfhgfhjg"
                          Html.a(ConductorComponent.getUrl(conductor, "prepare", ActorRun.getRootInstance(project)),
                            Html.span("right badge badge-secondary", null, "run")
                          ),
                          Html.javascript("updateConductorJobNum('" + conductor.getLocalDirectoryPath().toString() + "'," + finalRunningCount + ")")
                        )
                      )*/)
                    );
                  } ));
                }
                return list;
              }
            })
            , null, "card-warning card-outline", "p-0");
        }

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void addConductor() {
    String name = request.queryParams("name");
    //AbstractConductor abstractConductor = AbstractConductor.getInstance(type);
    Conductor conductor = Conductor.create(project, name);
    response.redirect(ConductorComponent.getUrl(conductor));
  }
}
