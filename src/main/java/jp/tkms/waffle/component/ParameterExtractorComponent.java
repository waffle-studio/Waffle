package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.exception.ProjectNotFoundException;
import jp.tkms.waffle.data.util.ResourceFile;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterExtractorComponent extends AbstractAccessControlledComponent {
  public static final String KEY_REMOVE = "remove";

  private Mode mode;

  private Project project;
  private Simulator simulator;
  private String extractorName;

  public ParameterExtractorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ParameterExtractorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null, null), new ParameterExtractorComponent());
    Spark.get(getStaticUrl(null, "add"), new ParameterExtractorComponent(Mode.Add));
    Spark.post(getStaticUrl(null, "add"), new ParameterExtractorComponent(Mode.Add));
    Spark.post(getUrl(null, null, "update"), new ParameterExtractorComponent(Mode.Update));
    Spark.get(getUrl(null, null, KEY_REMOVE), new ParameterExtractorComponent(Mode.Remove));
  }

  public static String getStaticUrl(Simulator simulator, String mode) {
    return "/parameter_extractor-" + mode + "/"
      + (simulator == null ? ":project/:simulator" : simulator.getProject().getName() + "/" + simulator.getName());
  }

  public static String getUrl(Simulator simulator, String name) {
    return "/parameter_extractor/"
      + (name == null ? ":project/:simulator/:name"
      : simulator.getProject().getName() + '/' + simulator.getName() + '/' +  name);
  }

  public static String getUrl(Simulator simulator, String name, String mode) {
    return getUrl(simulator, name) + '/' + mode;
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));

    switch (mode) {
      case Add:
        if (isPost()) {
          System.out.println("OK");
          addParameterExtractor();
        } else {
          renderAddParameterExtractorForm();
        }
        break;
      case Update:
        extractorName = request.params("name");
        updateParameterExtractor();
        break;
      case Remove:
        extractorName = request.params("name");
        removeParameterExtractor();
        break;
      default:
        extractorName = request.params("name");
        renderParameterExtractor();
    }
  }

  private void renderParameterExtractor() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return extractorName;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getName()),
          "Parameter Extractor",
          extractorName
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getUrl(simulator, extractorName, "update"), Html.Method.Post,
          Lte.card(Html.fasIcon("tasks") + "Properties", null,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", extractorName, errors),
              Lte.formDataEditorGroup("extract_script", "Script", "ruby", simulator.getExtractorScript(extractorName), errors)
            )
            , Lte.formSubmitButton("primary", "Update")
          )
        );

        content += Html.form(getUrl(simulator, extractorName, KEY_REMOVE), Html.Method.Get,
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

  private void renderAddParameterExtractorForm() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Parameter Extractor";
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
          "Parameter Extractor",
          "Add"
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getStaticUrl(simulator,"add"), Html.Method.Post,
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

  public void addParameterExtractor() {
    String name = request.queryParams("name");
    simulator.createExtractor(name);
    response.redirect(getUrl(simulator, name));
  }

  public void updateParameterExtractor() {
    String name = request.queryParams("name");
    String script = request.queryParams("extract_script");
    simulator.updateExtractorScript(name, script);
    response.redirect(getUrl(simulator, extractorName));
  }

  public void removeParameterExtractor() {
    simulator.removeExtractor(extractorName);
    response.redirect(SimulatorComponent.getUrl(simulator));
  }

  public enum Mode {Default, Add, Update, Remove}
}
