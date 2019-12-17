package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterGroupComponent extends AbstractComponent {
  static final String MODE_ADD_PARAMETER_GROUP = "add_parameter_group";
  static final String MODE_ADD_PARAMETER = "add_parameter";

  private Mode mode;

  private Project project;
  private Simulator simulator;
  private ParameterGroup group;
  public ParameterGroupComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ParameterGroupComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ParameterGroupComponent());
    Spark.get(getUrl(null, MODE_ADD_PARAMETER_GROUP), new ParameterGroupComponent(Mode.AddParameterGroup));
    Spark.post(getUrl(null, MODE_ADD_PARAMETER_GROUP), new ParameterGroupComponent(Mode.AddParameterGroup));
    Spark.get(getUrl(null, MODE_ADD_PARAMETER), new ParameterGroupComponent(Mode.AddParameter));
    Spark.post(getUrl(null, MODE_ADD_PARAMETER), new ParameterGroupComponent(Mode.AddParameter));

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(ParameterGroup group) {
    return "/parameter_mode_group/"
      + (group == null ? ":project/:simulator/:id" : group.getSimulator().getProject().getId() + '/' + group.getSimulator().getId() + '/' + group.getId());
  }

  public static String getUrl(ParameterGroup group, String mode) {
    return getUrl(group) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));
    group = ParameterGroup.getInstance(simulator, request.params("id"));

    switch (mode) {
      case AddParameterGroup:
        if (isPost()) {
          ArrayList<Lte.FormError> errors = checkParameterGroupAddFormError();
          if (errors.isEmpty()) {
            addParameterGroup();
          } else {
            renderParameterGroupAddForm(checkParameterGroupAddFormError());
          }
        } else {
          renderParameterGroupAddForm(new ArrayList<>());
        }
        break;
      case AddParameter:
        if (isPost()) {
          ArrayList<Lte.FormError> errors = checkParameterAddFormError();
          if (errors.isEmpty()) {
            addParameter();
          } else {
            renderParameterGroupAddForm(checkParameterAddFormError());
          }
        } else {
          renderParameterAddForm(new ArrayList<>());
        }
        break;
      default:
        renderParameterModelGroup();
        break;
    }
  }

  private void renderParameterModelGroup() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        if (group.isRoot()) {
          return "Parameter Model";
        }
        return group.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId())
        ));
        if (group.isRoot()) {
          breadcrumb.add("Parameter Model");
        } else {
          breadcrumb.add(Html.a(ParameterGroupComponent.getUrl(ParameterGroup.getRootInstance(simulator)),
            "Parameter Model"
          ));
          breadcrumb.add(group.getId());
        }
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        if (! group.isRoot()) {
          content += Lte.card(Html.faIcon("tasks") + "Properties",
            null,
            Html.div(null,
              Html.inputHidden("cmd", "add"),
              Lte.formInputGroup("text", "instance_size", "Number of instance", "", group.getInstanceSize().toString(), errors)
            )
            , null);
        }

        content += Lte.card(Html.faIcon("list-alt") + "Parameter Models",
          Html.a(getUrl(group, MODE_ADD_PARAMETER), Html.faIcon("plus")),
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
              for (Parameter model : Parameter.getList(group)) {
                list.add(new Lte.TableRow(
                  Html.a(ParameterComponent.getUrl(model), null, null, model.getShortId()),
                  model.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("list-alt") + "Parameter Model Groups",
          Html.a(getUrl(group, MODE_ADD_PARAMETER_GROUP), Html.faIcon("plus-square")),
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
              for (ParameterGroup group : ParameterGroup.getList(group)) {
                list.add(new Lte.TableRow(
                  Html.a(getUrl(group), null, null, group.getShortId()),
                  group.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        return content;
      }
    }.render(this);
  }

  private void renderParameterGroupAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return group.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "Add Group";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          Html.a(ParameterGroupComponent.getUrl(ParameterGroup.getRootInstance(simulator)),
            "Parameter Model"
          ),
          Html.a(ParameterGroupComponent.getUrl(group), group.getShortId()),
          "Add Group"
        ));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(group, MODE_ADD_PARAMETER_GROUP), Html.Method.Post,
            Lte.card("New Parameter Model Group", null,
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

  private ArrayList<Lte.FormError> checkParameterGroupAddFormError() {
    return new ArrayList<>();
  }

  private void addParameterGroup() {
    ParameterGroup group = ParameterGroup.create(simulator, this.group, request.queryParams("name"));
    response.redirect(getUrl(group));
  }

  private void renderParameterAddForm(ArrayList<Lte.FormError> errors) {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return group.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "Add Parameter";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          Html.a(ParameterGroupComponent.getUrl(ParameterGroup.getRootInstance(simulator)),
            "Parameter Model"
          ),
          Html.a(ParameterGroupComponent.getUrl(group), group.getShortId()),
          "Add Parameter"
        ));
      }

      @Override
      protected String pageContent() {
        return
          Html.form(getUrl(group, MODE_ADD_PARAMETER), Html.Method.Post,
            Lte.card("New Parameter", null,
              Html.div(null,
                Html.inputHidden("cmd", "add"),
                Lte.formInputGroup("text", "name", null, "Name", null, errors),
                Html.div("form-group clearfix",
                  Html.div("icheck-primary d-inline",
                    Html.attribute("input",
                      Html.value("type", "radio"),
                      Html.value("id", "value_type_c"),
                      Html.value("name", "value_type"),
                      Html.value("value", "categorical"), "checked"),
                    Html.element("label", new Html.Attributes(Html.value("for", "value_type_c")), "Categorical")
                    ),
                  Html.div("d-inline","&nbsp;"),
                  Html.div("icheck-primary d-inline",
                    Html.attribute("input",
                      Html.value("type", "radio"),
                      Html.value("id", "value_type_q"),
                      Html.value("name", "value_type"),
                      Html.value("value", "quantitative")),
                    Html.element("label", new Html.Attributes(Html.value("for", "value_type_q")), "Quantitative")
                  )
                )
              ),
              Lte.formSubmitButton("success", "Add"),
              "card-warning", null
            )
          );
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkParameterAddFormError() {
    return new ArrayList<>();
  }

  private void addParameter() {
    Parameter parameter = Parameter.create(group, request.queryParams("name"));
    parameter.isQuantitative(request.queryParams("value_type").equals("quantitative"));
    response.redirect(ParameterComponent.getUrl(parameter));
  }

  public enum Mode {Default, AddParameterGroup, AddParameter}
}
