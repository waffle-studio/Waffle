package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectsComponent extends AbstractComponent {
  private Mode mode;

  ;

  public ProjectsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ProjectsComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new ProjectsComponent());
    Spark.get(getUrl("add"), new ProjectsComponent(ProjectsComponent.Mode.Add));
    Spark.post(getUrl("add"), new ProjectsComponent(ProjectsComponent.Mode.Add));

    ProjectComponent.register();
  }

  public static String getUrl() {
    return "/projects";
  }

  public static String getUrl(String mode) {
    return "/projects/" + mode;
  }

  @Override
  public void controller() {
    if (mode == Mode.Add) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkAddFormError();
        if (errors.isEmpty()) {
          addProject();
        } else {
          renderAddForm(errors);
        }
      } else {
        renderAddForm(new ArrayList<>());
      }
    } else {
      renderProjectList();
    }
  }

  private void renderAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Projects";
      }

      @Override
      protected String pageSubTitle() {
        return "Add";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(getUrl(), null, null, "Projects"),
          "Add")
        );
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl("add"), Html.Method.Post,
            Lte.card("New Project", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkAddFormError() {
    return new ArrayList<>();
  }

  private void renderProjectList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Projects";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList("Projects"));
      }

      @Override
      protected String pageContent() {
        ArrayList<Project> projectList = Project.getList();
        if (projectList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl("add"), null, null,
              Html.faIcon("plus-square") + "Add new project"
            ),
            null
          );
        }
        return Lte.card(null,
          Html.a(getUrl("add"),
            null, null, Html.faIcon("plus-square")
          ),
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
    for (Project project : Project.getList()) {
      list.add(new Lte.TableRow(
        Html.a(ProjectComponent.getUrl(project), null, null, project.getShortId()),
        project.getName())
      );
    }
    return list;
  }

  private void addProject() {
    String name = request.queryParams("name");
    Project project = Project.create(name);
    response.redirect(ProjectComponent.getUrl(project));
  }

  public enum Mode {Default, Add}
}
