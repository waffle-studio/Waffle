package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Trials;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class TrialsComponent extends AbstractComponent {
  private Mode mode;

  ;
  private Project project;
  private Trials trials;
  public TrialsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public TrialsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new TrialsComponent());
  }

  public static String getUrl(Project project) {
    return "/trials/" + (project == null ? ":project/:id" : project.getId() + "/ROOT");
  }

  public static String getUrl(Project project, String mode) {
    return getUrl(project) + "/" + mode;
  }

  @Override
  public void controller() {
    project = new Project(request.params("project"));

    renderProjectsList();
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderProjectsList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Trials";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Trials",
          request.params("id")
        ));
      }

      @Override
      protected String pageContent() {
        ArrayList<Project> projectList = Project.getProjectList();
        return Lte.card(null, null,
          Lte.table("table-condensed", getProjectTableHeader(), getProjectTableRow())
          , null, null, "p-0");
      }
    }.render(this);
  }

  private ArrayList<Lte.TableHeader> getProjectTableHeader() {
    ArrayList<Lte.TableHeader> list = new ArrayList<>();
    list.add(new Lte.TableHeader("width:8em;", "ID"));
    list.add(new Lte.TableHeader("", "Name"));
    return list;
  }

  private ArrayList<Lte.TableRow> getProjectTableRow() {
    ArrayList<Lte.TableRow> list = new ArrayList<>();
    for (Project project : Project.getProjectList()) {
      list.add(new Lte.TableRow(
        Html.a("/project/" + project.getId(), null, null, project.getShortId()),
        project.getName())
      );
    }
    return list;
  }

  public enum Mode {Default}
}
