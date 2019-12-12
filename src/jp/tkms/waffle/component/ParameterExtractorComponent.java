package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class ParameterExtractorComponent extends AbstractComponent {
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

    SimulatorsComponent.register();
    TrialsComponent.register();
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
    extractor = ParameterExtractor.getInstance(simulator, request.params("id"));

    renderParameterModel();
  }

  private void renderParameterModel() {
    new MainTemplate() {
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
          Html.div(null,
            Html.inputHidden("cmd", "add"),
            Lte.formTextAreaGroup("extract_script", "Extract script", 8, extractor.getScript(), errors)
          )
          , null);

        return content;
      }
    }.render(this);
  }

  public enum Mode {Default}
}
