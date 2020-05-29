package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.ResourceFile;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterExtractorComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  private Project project;
  private Simulator simulator;
  private ParameterExtractor extractor;

  public ParameterExtractorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ParameterExtractorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ParameterExtractorComponent());
    Spark.get(getStaticUrl(null, "add"), new ParameterExtractorComponent(Mode.Add));
    Spark.post(getStaticUrl(null, "add"), new ParameterExtractorComponent(Mode.Add));
    Spark.post(getUrl(null, "update"), new ParameterExtractorComponent(Mode.Update));

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getStaticUrl(Simulator simulator, String mode) {
    return "/parameter_extractor-" + mode + "/"
      + (simulator == null ? ":project/:simulator" : simulator.getProject().getId() + "/" + simulator.getId());
  }

  public static String getUrl(ParameterExtractor extractor) {
    return "/parameter_extractor/"
      + (extractor == null ? ":project/:simulator/:id"
      : extractor.getSimulator().getProject().getId() + '/' + extractor.getSimulator().getId() + '/' +  extractor.getId());
  }

  public static String getUrl(ParameterExtractor extractor, String mode) {
    return getUrl(extractor) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    simulator = Simulator.getInstance(project, request.params("simulator"));

    if (mode.equals(Mode.Default) || mode.equals(Mode.Update)) {
      extractor = ParameterExtractor.getInstance(simulator, request.params("id"));
    }

    switch (mode) {
      case Add:
        if (isPost()) {
          addParameterExtractor();
        } else {
          renderAddParameterExtractorForm();
        }
        break;
      case Update:
        updateParameterExtractor();
        break;
      default:
        renderParameterExtractor();
    }
  }

  private void renderParameterExtractor() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return extractor.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          "Parameter Extractor",
          extractor.getId()
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("tasks") + "Properties",
          null,
          Html.form(getUrl(extractor, "update"), Html.Method.Post,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", extractor.getName(), errors),
              Lte.formDataEditorGroup("extract_script", "Extract script", "ruby", extractor.getScript(), errors),
              Lte.formSubmitButton("primary", "Update")
            )
          )
          , null);


        return content;
      }
    }.render(this);
  }

  private void renderAddParameterExtractorForm() {
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
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          Html.a(SimulatorsComponent.getUrl(project), "Simulators"),
          Html.a(SimulatorComponent.getUrl(simulator), simulator.getShortId()),
          "Parameter Extractor",
          "Add"
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Lte.card(Html.faIcon("tasks") + "Properties",
          null,
          Html.form(getUrl(extractor, "add"), Html.Method.Post,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", "", errors),
              Lte.formDataEditorGroup("extract_script", "Extract script", "ruby", ResourceFile.getContents("/default_parameter_extractor.rb"), errors),
              Lte.formSubmitButton("success", "Add")
            )
          )
          , null);


        return content;
      }
    }.render(this);
  }

  public void addParameterExtractor() {
    String name = request.queryParams("name");
    String script = request.queryParams("extract_script");
    ParameterExtractor extractor = ParameterExtractor.create(simulator, name, script);
    response.redirect(getUrl(extractor));
  }

  public void updateParameterExtractor() {
    String name = request.queryParams("name");
    String script = request.queryParams("extract_script");
    extractor.update(name, script);
    response.redirect(getUrl(extractor));
  }

  public enum Mode {Default, Add, Update}
}
