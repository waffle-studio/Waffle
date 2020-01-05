package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Parameter;
import jp.tkms.waffle.data.ParameterGroup;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Simulator;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  private Project project;
  private Simulator simulator;
  private Parameter parameter;

  public ParameterComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ParameterComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ParameterComponent());
    Spark.post(getUrl(null, "update"), new ParameterComponent(Mode.Update));

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(Parameter parameter) {
    return "/parameter_model/"
      + (parameter == null ? ":project/:simulator/:id" : parameter.getSimulator().getProject().getId() + '/' + parameter.getSimulator().getId() + '/' +  parameter.getId());
  }

  public static String getUrl(Parameter parameter, String mode) {
    return getUrl(parameter) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));
    parameter = Parameter.getInstance(simulator, request.params("id"));

    switch (mode) {
      case Update:
        updateParameter();
        break;
      default:
        renderParameter();
    }
  }

  private void renderParameter() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return parameter.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          Html.a(ParameterGroupComponent.getUrl(ParameterGroup.getRootInstance(simulator)),
            "Parameter Model"
          )
        ));
        if (!parameter.getParent().isRoot()) {
          breadcrumb.add(Html.a(ParameterGroupComponent.getUrl(parameter.getParent()), parameter.getParent().getShortId()));
        }
        breadcrumb.add(parameter.getId());
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("tasks") + "Properties",
          null,
          Html.form(getUrl(parameter, "update"), Html.Method.Post,
            Html.div(null,
              Html.inputHidden("cmd", "add"),
              Html.div("form-group clearfix",
                Html.div("icheck-primary d-inline",
                  Html.attribute("input",
                    Html.value("type", "radio"),
                    Html.value("id", "value_type_c"),
                    Html.value("name", "value_type"),
                    Html.value("value", "categorical"), (parameter.isQuantitative()?"":"checked")),
                  Html.element("label", new Html.Attributes(Html.value("for", "value_type_c")), "Categorical")
                ),
                Html.div("d-inline","&nbsp;"),
                Html.div("icheck-primary d-inline",
                  Html.attribute("input",
                    Html.value("type", "radio"),
                    Html.value("id", "value_type_q"),
                    Html.value("name", "value_type"),
                    Html.value("value", "quantitative"), (parameter.isQuantitative()?"checked":"")),
                  Html.element("label", new Html.Attributes(Html.value("for", "value_type_q")), "Quantitative")
                )
              ),
              Lte.formInputGroup("text", "default_value", "Default value", "Name", parameter.getDefaultValue(), errors),
              Lte.formDataEditorGroup("update_script", "Default value update script", "ruby", parameter.getDefaultValueUpdateScript(), errors),
              Lte.formSubmitButton("primary", "Update")
            )
          )
          , null);

        return content;
      }
    }.render(this);
  }

  void updateParameter() {
    parameter.isQuantitative(request.queryParams("value_type").equals("quantitative"));
    parameter.setDefaultValue(request.queryParams("default_value"));
    parameter.setDefaultValueUpdateScript(request.queryParams("update_script"));
    response.redirect(getUrl(parameter));
  }

  public enum Mode {Default, Update}
}
