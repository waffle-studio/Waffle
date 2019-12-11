package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.ParameterModel;
import jp.tkms.waffle.data.ParameterModelGroup;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Simulator;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterModelComponent extends AbstractComponent {
  private Mode mode;

  private Project project;
  private Simulator simulator;
  private ParameterModel parameter;

  public ParameterModelComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ParameterModelComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ParameterModelComponent());

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(ParameterModel parameter) {
    return "/parameter_model/"
      + (parameter == null ? ":project/:simulator/:id" : parameter.getSimulator().getProject().getId() + '/' + parameter.getSimulator().getId() + '/' +  parameter.getId());
  }

  public static String getUrl(ParameterModel parameter, String mode) {
    return getUrl(parameter) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));
    parameter = ParameterModel.getInstance(simulator, request.params("id"));

    renderParameterModel();
  }

  private void renderParameterModel() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return parameter.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          Html.a(ParameterModelGroupComponent.getUrl(ParameterModelGroup.getRootInstance(simulator)),
            "Parameter Model"
          ),
          Html.a(ParameterModelGroupComponent.getUrl(parameter.getParent()), parameter.getParent().getShortId()),
          parameter.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("tasks") + "Properties",
          null,
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
            Lte.formInputGroup("text", "name", "Default value", "Name", parameter.getShortId(), errors),
            Lte.formTextAreaGroup("update_script", "Default value update script", 8, "", errors)
          )
          , null);

        return content;
      }
    }.render(this);
  }

  public enum Mode {Default}
}
