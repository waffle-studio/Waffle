package jp.tkms.waffle.web.component;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

public class SimulatorsComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  private String requestedId;
  private Project project;
  public SimulatorsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public SimulatorsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new SimulatorsComponent());
    Spark.get(getUrl(null, "add"), new SimulatorsComponent(Mode.Add));
    Spark.post(getUrl(null, "add"), new SimulatorsComponent(Mode.Add));
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + Executable.EXECUTABLE;
  }

  public static String getUrl(Project project, String mode) {
    return getUrl(project) + "/@" + mode;
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    requestedId = request.params("project");
    project = Project.getInstance(requestedId);

    if (mode == Mode.Add) {
      if (request.requestMethod().toLowerCase().equals("post")) {
        ArrayList<Lte.FormError> errors = checkCreateProjectFormError();
        if (errors.isEmpty()) {
          addSimulator();
        } else {
          renderAddForm(errors);
        }
      } else {
        renderAddForm(new ArrayList<>());
      }
    } else {
      renderSimulatorList();
    }
  }

  private void renderAddForm(ArrayList<Lte.FormError> errors) throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Simulators";
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
          Html.a(SimulatorsComponent.getUrl(project), "Simulators")));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(project, "add"), Html.Method.Post,
            Lte.card("New Simulator", null,
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

  private void addSimulator() {
    Executable executable = Executable.create(project, request.queryParams("name"));
    executable.setSimulatorCommand("");
    response.redirect(SimulatorComponent.getUrl(executable));
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderSimulatorList() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Simulators";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName())));
      }

      @Override
      protected String pageContent() {
        ArrayList<Executable> executableList = Executable.getList(project);
        if (executableList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl(project, "add"), null, null,
              Html.fasIcon("plus-square") + "Add Simulator"
            ),
            null
          );
        }
        return Lte.card(null,
          Html.a(getUrl(project, "add"),
            null, null, Html.fasIcon("plus-square")
          ),
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
              for (Executable executable : executableList) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                  return new Lte.TableRow(
                      Html.a(SimulatorComponent.getUrl(executable), null, null, executable.getName()));
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

  public enum Mode {Default, Add}
}
