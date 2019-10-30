package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectComponent extends AbstractComponent {
  private Mode mode;

  ;
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
    Spark.get(getUrl(null, "edit_const_model"), new ProjectComponent());

    SimulatorsComponent.register();
    SimulatorComponent.register();
    TrialsComponent.register();
    ConductorComponent.register();
  }

  public static String getUrl(Project project) {
    return "/project/" + (project == null ? ":id" : project.getId());
  }

  public static String getUrl(Project project, String mode) {
    return getUrl(project) + '/' + mode;
  }

  @Override
  public void controller() {
    requestedId = request.params("id");
    project = Project.getInstance(requestedId);

    if (!project.isValid()) {
      mode = Mode.NotFound;
    }

    if (mode == Mode.NotFound) {
      renderProjectNotFound();
    } else {
      renderProject();
    }
  }

  private void renderProjectNotFound() {
    new MainTemplate() {
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
          Html.h1("text-center", Html.faIcon("question")),
          null
        );
      }
    }.render(this);
  }

  private void renderProject() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return project.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"), project.getId()));
      }

      @Override
      protected String pageContent() {
        String content = Lte.divRow(
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "layer-group", "bg-info",
            Html.a(SimulatorsComponent.getUrl(project), "Simulators"), ""),
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "project-diagram", "bg-danger",
            Html.a(TrialsComponent.getUrl(project), "Trials"), "")
        );

        content += Lte.card(Html.faIcon("user-tie") + "Conductors", null,
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:8em;", "ID"));
              list.add(new Lte.TableValue("", "Name"));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (Conductor conductor : Conductor.getList(project)) {
                list.add(new Lte.TableRow(
                  new Lte.TableValue("",
                    Html.a(ConductorComponent.getUrl(conductor),
                      null, null, conductor.getShortId())),
                  new Lte.TableValue("", conductor.getName()),
                  new Lte.TableValue("text-align:right;",
                    Html.a(ConductorComponent.getUrl(conductor, "run"),
                      Html.span("right badge badge-secondary", null, "run")
                    )
                  )
                ));
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("list") + "Constant sets", null, null,
          Html.a(getUrl(project, "edit_const_model"), Html.faIcon("pencil") + "Edit model"));

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  public enum Mode {Default, NotFound, EditConstModel}
}
