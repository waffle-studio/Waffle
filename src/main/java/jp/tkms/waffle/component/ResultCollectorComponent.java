package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Simulator;
import jp.tkms.waffle.data.util.ResourceFile;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ResultCollectorComponent extends AbstractAccessControlledComponent {
  public static final String KEY_REMOVE = "remove";
  private Mode mode;

  private Project project;
  private Simulator simulator;
  private String collectorName;

  public ResultCollectorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ResultCollectorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null, null), new ResultCollectorComponent());
    Spark.get(getStaticUrl(null, "add"), new ResultCollectorComponent(Mode.Add));
    Spark.post(getStaticUrl(null, "add"), new ResultCollectorComponent(Mode.Add));
    Spark.post(getUrl(null, null, "update"), new ResultCollectorComponent(Mode.Update));
    Spark.get(getUrl(null, null, KEY_REMOVE), new ResultCollectorComponent(Mode.Remove));
  }

  public static String getStaticUrl(Simulator simulator, String mode) {
    return "/result_collector-" + mode + "/"
      + (simulator == null ? ":project/:simulator" : simulator.getProject().getName() + "/" + simulator.getName());
  }

  public static String getUrl(Simulator simulator, String name) {
    return "/result_collector/"
      + (name == null ? ":project/:simulator/:name"
      : simulator.getProject().getName() + '/' + simulator.getName() + '/' +  name);
  }

  public static String getUrl(Simulator simulator, String name, String mode) {
    return getUrl(simulator, name) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));

    switch (mode) {
      case Add:
        if (isPost()) {
          addResultCollector();
        } else {
          renderAddParameterExtractorForm();
        }
        break;
      case Update:
        collectorName = request.params("name");
        updateResultCollector();
        break;
      case Remove:
        collectorName = request.params("name");
        removeResultCollector();
        break;
      default:
        collectorName = request.params("name");
        renderParameterExtractor();
    }
  }

  private void renderParameterExtractor() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return collectorName;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getName()),
          "Result Collector",
          collectorName
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getUrl(simulator, collectorName, "update"), Html.Method.Post,
          Lte.card(Html.fasIcon("tasks") + "Properties", null,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", collectorName, errors),
              Lte.formDataEditorGroup("collect_script", "Script", "ruby", simulator.getCollectorScript(collectorName), errors)
              )
            , Lte.formSubmitButton("primary", "Update")
          )
        );

        content += Html.form(getUrl(simulator, collectorName, KEY_REMOVE), Html.Method.Get,
          Lte.card(Html.fasIcon("trash-alt") + "Remove",
            Lte.cardToggleButton(true),
            Html.div(null,
              Lte.formSubmitButton("danger", "Remove")
            )
            , null, "collapsed-card", null
          )
        );


        return content;
      }
    }.render(this);
  }

  private void renderAddParameterExtractorForm() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Result Collector";
      }

      @Override
      protected String pageSubTitle() {
        return "Add";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getName()),
          "Result Collector",
          "Add"
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getStaticUrl(simulator,  "add"), Html.Method.Post,
          Lte.card(Html.fasIcon("tasks") + "Properties",
            null,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", "", errors)
            )
            , Lte.formSubmitButton("success", "Add")
          )
        );


        return content;
      }
    }.render(this);
  }

  public void addResultCollector() {
    String name = request.queryParams("name");
    simulator.createCollector(name);
    response.redirect(getUrl(simulator, name));
  }

  public void updateResultCollector() {
    String name = request.queryParams("name");
    String script = request.queryParams("collect_script");
    simulator.updateCollectorScript(name, script);
    response.redirect(getUrl(simulator, collectorName));
  }

  public void removeResultCollector() {
    simulator.removeCollector(collectorName);
    response.redirect(SimulatorComponent.getUrl(simulator));
  }

  public enum Mode {Default, Add, Update, Remove}
}
