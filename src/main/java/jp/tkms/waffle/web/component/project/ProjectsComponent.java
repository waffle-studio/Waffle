package jp.tkms.waffle.web.component.project;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.exception.InvalidInputException;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.MainTemplate;
import jp.tkms.waffle.data.project.Project;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

public class ProjectsComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  public ProjectsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ProjectsComponent() {
    this(Mode.Default);
  }

  public static void register() {
    Spark.get(getUrl(), new ProjectsComponent());
    Spark.get(getUrl(Mode.New), new ProjectsComponent(ProjectsComponent.Mode.New));
    Spark.post(getUrl(Mode.New), new ProjectsComponent(ProjectsComponent.Mode.New));

    ProjectComponent.register();
  }

  public static String getUrl() {
    return "/PROJECT";
  }

  public static String getUrl(Mode mode) {
    return getUrl() + "/@" + mode.name();
  }

  @Override
  public void controller() {
    if (mode == Mode.New) {
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
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Projects";
      }

      @Override
      protected String pageSubTitle() {
        return "@New";
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
          Html.form(getUrl(Mode.New), Html.Method.Post,
            Lte.card("New Project", null,
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

  private ArrayList<Lte.FormError> checkAddFormError() {
    return new ArrayList<>();
  }

  private void renderProjectList() {
    new MainTemplate() {

      @Override
      protected boolean enableParentLink() {
        return false;
      }

      @Override
      protected ArrayList<Map.Entry<String, String>> pageNavigation() {
        return null;
      }

      @Override
      protected String pageTitle() {
        return "Projects";
      }

      @Override
      protected String helpLink() {
        return "project";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList("Projects"));
      }

      @Override
      protected String pageTool() {
        return Html.a(getUrl(Mode.New),
          null, null, Lte.badge("primary", null, Html.fasIcon("plus-square") + "NEW")
        );
      }

      @Override
      protected String pageContent() {
        ArrayList<Project> projectList = Project.getList();
        if (projectList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl(Mode.New), null, null,
              Html.fasIcon("plus-square") + "Add new project"
            ),
            null
          );
        }

        return Lte.card(null, null,
          Lte.table("table-condensed table-nooverflow", new Lte.Table() {
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
              for (Project project : Project.getList()) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    return new Lte.TableRow(
                      Html.a(ProjectComponent.getUrl(project), null, null, project.getName()),
                      project.getNote()
                    );
                  }
                ));
              }
              return list;
            }
          })
          , null, "card-outline card-secondary", "p-0");
      }
    }.render(this);
  }

  private void addProject() {
    String name = request.queryParams("name");
    Project project = null;
    try {
      project = Project.create(name);
    } catch (InvalidInputException e) {
      response.redirect(ProjectsComponent.getUrl());
      return;
    }
    response.redirect(ProjectComponent.getUrl(project));
  }

  public enum Mode {Default, New}
}
