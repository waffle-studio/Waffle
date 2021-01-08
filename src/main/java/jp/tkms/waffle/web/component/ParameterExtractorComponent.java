package jp.tkms.waffle.web.component;

import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

import static jp.tkms.waffle.data.project.executable.Executable.KEY_EXTRACTOR;

public class ParameterExtractorComponent extends AbstractAccessControlledComponent {
  public static final String KEY_REMOVE = "remove";

  private Mode mode;

  private Project project;
  private Executable executable;
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

  public static String getStaticUrl(Executable executable, String mode) {
    return SimulatorComponent.getUrl(executable) + "/" + KEY_EXTRACTOR + "/@" + (mode == null ? ":mode" : mode);
  }

  public static String getUrl(Executable executable, String name) {
    return SimulatorComponent.getUrl(executable) + "/" + KEY_EXTRACTOR + "/" + (name == null ? ":name" : name + ".rb");
  }

  public static String getUrl(Executable executable, String name, String mode) {
    return getUrl(executable, name) + "/@" + mode;
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    executable = Executable.getInstance(project, request.params("simulator"));

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
        extractorName = request.params("name").replaceFirst("\\.rb$", "");
        updateParameterExtractor();
        break;
      case Remove:
        extractorName = request.params("name").replaceFirst("\\.rb$", "");
        removeParameterExtractor();
        break;
      default:
        extractorName = request.params("name").replaceFirst("\\.rb$", "");
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
          Html.a(SimulatorComponent.getUrl(executable), executable.getName()),
          "Parameter Extractor",
          extractorName
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getUrl(executable, extractorName, "update"), Html.Method.Post,
          Lte.card(Html.fasIcon("tasks") + "Properties", null,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", extractorName, errors),
              Lte.formDataEditorGroup("extract_script", "Script", "ruby", executable.getExtractorScript(extractorName), errors)
            )
            , Lte.formSubmitButton("primary", "Update")
          )
        );

        content += Html.form(getUrl(executable, extractorName, KEY_REMOVE), Html.Method.Get,
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
          Html.a(SimulatorComponent.getUrl(executable), executable.getName()),
          "Parameter Extractor",
          "Add"
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getStaticUrl(executable,"add"), Html.Method.Post,
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
    String name = request.queryParams("name").replaceFirst("\\.rb$", "");
    executable.createExtractor(name);
    response.redirect(getUrl(executable, name));
  }

  public void updateParameterExtractor() {
    String name = request.queryParams("name").replaceFirst("\\.rb$", "");
    String script = request.queryParams("extract_script");
    executable.updateExtractorScript(name, script);
    response.redirect(getUrl(executable, extractorName));
  }

  public void removeParameterExtractor() {
    executable.removeExtractor(extractorName);
    response.redirect(SimulatorComponent.getUrl(executable));
  }

  public enum Mode {Default, Add, Update, Remove}
}
