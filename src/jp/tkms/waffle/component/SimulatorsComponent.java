package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Simulator;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class SimulatorsComponent extends AbstractComponent {
  private Mode mode;

  ;
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
    return "/simulators/" + (project == null ? ":project" : project.getId());
  }

  public static String getUrl(Project project, String mode) {
    return getUrl(project) + '/' + mode;
  }

  @Override
  public void controller() {
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

  private void renderAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Simulators";
      }

      @Override
      protected String pageSubTitle() {
        return "Add";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          "Add"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(project, "add"), Html.Method.Post,
            Lte.card("New Simulator", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors),
                Html.hr(),
                Lte.formInputGroup("text", "sim_cmd", "Simulation command", "", null, errors),
                Lte.formInputGroup("text", "ver_cmd", "Version command", "", null, errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private void addSimulator() {
    Simulator simulator = Simulator.create(project,
      request.queryParams("name"),
      request.queryParams("sim_cmd"),
      request.queryParams("ver_cmd")
    );
    response.redirect(SimulatorComponent.getUrl(simulator));
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderSimulatorList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Simulators";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Simulators"));
      }

      @Override
      protected String pageContent() {
        ArrayList<Simulator> simulatorList = Simulator.getList(project);
        if (simulatorList.size() <= 0) {
          return Lte.card(null, null,
            Html.a(getUrl(project, "add"), null, null,
              Html.faIcon("plus-square") + "Add simulator"
            ),
            null
          );
        }
        return Lte.card(null,
          Html.a(getUrl(project, "add"),
            null, null, Html.faIcon("plus-square")
          ),
          Lte.table("table-condensed", new Lte.Table() {
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
              for (Simulator simulator : simulatorList) {
                list.add(new Lte.TableRow(
                  Html.a(SimulatorComponent.getUrl(simulator), null, null, simulator.getShortId()),
                  simulator.getName())
                );
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
