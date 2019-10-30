package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
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

        content += Lte.card(Html.faIcon("user-tie") + "Conductors", null, null, null);

        content += Lte.card(Html.faIcon("list") + "Constant sets", null, null,
          Html.a(getUrl(project, "edit_const_model"), Html.faIcon("pencil") + "Edit model"));

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private ArrayList<Lte.TableHeader> getProjectTableHeader() {
    ArrayList<Lte.TableHeader> list = new ArrayList<>();
    list.add(new Lte.TableHeader("width:8em;", "ID"));
    list.add(new Lte.TableHeader("", "Name"));
    return list;
  }

  private ArrayList<Lte.TableRow> getProjectTableRow() {
    ArrayList<Lte.TableRow> list = new ArrayList<>();
    for (Project project : Project.getList()) {
      list.add(new Lte.TableRow(
        Html.a("", null, null, project.getShortId()),
        project.getName())
      );
    }
    return list;
  }

  private void createProject() {

  }

  public enum Mode {Default, NotFound, EditConstModel}
}
