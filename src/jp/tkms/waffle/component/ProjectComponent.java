package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.conductor.AbstractConductor;
import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Registry;
import jp.tkms.waffle.data.Trial;
import jp.tkms.waffle.data.util.KeyValue;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ProjectComponent extends AbstractComponent {
  public enum Mode {Default, NotFound, EditConstModel, AddConductor}
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
    Spark.get(getUrl(null, "add_conductor"), new ProjectComponent(Mode.AddConductor));
    Spark.post(getUrl(null, "add_conductor"), new ProjectComponent(Mode.AddConductor));

    SimulatorsComponent.register();
    SimulatorComponent.register();
    TrialsComponent.register();
    ConductorComponent.register();
    RunComponent.register();
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

  private void renderConductorAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Conductors";
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
          Html.a(SimulatorsComponent.getUrl(project), "Conductors"),
          "Add"));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(project, "add_conductor"), Html.Method.Post,
            Lte.card("New Conductor", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors),
                Lte.formSelectGroup("type", "type", Conductor.getConductorNameList(), errors)
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
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
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "project-diagram", "bg-danger",
            Html.a(TrialsComponent.getUrl(project), "Trials"), ""),
          Lte.infoBox(Lte.DivSize.F12Md12Sm6, "layer-group", "bg-info",
            Html.a(SimulatorsComponent.getUrl(project), "Simulators"), "")
        );

        ArrayList<Conductor> conductorList = Conductor.getList(project);
        if (conductorList.size() <= 0) {
          content += Lte.card(Html.faIcon("user-tie") + "Conductors",
            null,
            Html.a(getUrl(project, "add_conductor"), null, null,
              Html.faIcon("plus-square") + "Add new conductor"
            ),
            null
          );
        } else {
          content += Lte.card(Html.faIcon("user-tie") + "Conductors",
            Html.a(getUrl(project, "add_conductor"),
              null, null, Html.faIcon("plus-square")
            ),
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
                      Html.a(ConductorComponent.getUrl(conductor, "prepare", Trial.getRootInstance(project)),
                        Html.span("right badge badge-secondary", null, "run")
                      )
                    )
                  ));
                }
                return list;
              }
            })
            , null, null, "p-0");
        }

        content += Lte.card(Html.faIcon("list") + "Registry", null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("", "Key"));
              list.add(new Lte.TableValue("", "Value"));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (KeyValue e : Registry.getList(project)) {
                list.add(new Lte.TableRow(
                  new Lte.TableValue("", e.getKey()),
                  new Lte.TableValue("", e.getValue())
                ));
              }
              return list;
            }
          })
          , null, null, "p-0");

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void addConductor() {
    String name = request.queryParams("name");
    String type = request.queryParams("type");
    AbstractConductor abstractConductor = AbstractConductor.getInstance(type);
    Conductor.create(project, name, abstractConductor, abstractConductor.defaultScriptName());
    response.redirect(ProjectComponent.getUrl(project));
  }
}
