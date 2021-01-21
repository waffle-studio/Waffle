package jp.tkms.waffle.web.component.project.executable;

import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

import static jp.tkms.waffle.data.project.executable.Executable.KEY_COLLECTOR;

public class ResultCollectorComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  private Project project;
  private Executable executable;
  private String collectorName;

  public ResultCollectorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ResultCollectorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getStaticUrl(null, Mode.Add), new ResultCollectorComponent(Mode.Add));
    Spark.post(getStaticUrl(null, Mode.Add), new ResultCollectorComponent(Mode.Add));
    Spark.get(getUrl(null, null), new ResultCollectorComponent());
    Spark.post(getUrl(null, null, Mode.Update), new ResultCollectorComponent(Mode.Update));
    Spark.get(getUrl(null, null, Mode.Remove), new ResultCollectorComponent(Mode.Remove));
  }

  public static String getStaticUrl(Executable executable, Mode mode) {
    return ExecutableComponent.getUrl(executable) + "/" + KEY_COLLECTOR + "/@" + (mode == null ? ":mode" : mode.name());
  }

  public static String getUrl(Executable executable, String name) {
    return ExecutableComponent.getUrl(executable) + "/" + KEY_COLLECTOR + "/" + (name == null ? ":name" : name + ".rb");
  }

  public static String getUrl(Executable executable, String name, Mode mode) {
    return getUrl(executable, name) + "/@" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    executable = Executable.getInstance(project, request.params("simulator"));

    switch (mode) {
      case Add:
        if (isPost()) {
          addResultCollector();
        } else {
          renderAddParameterExtractorForm();
        }
        break;
      case Update:
        collectorName = request.params("name").replaceFirst("\\.rb$", "");
        updateResultCollector();
        break;
      case Remove:
        collectorName = request.params("name").replaceFirst("\\.rb$", "");
        removeResultCollector();
        break;
      default:
        collectorName = request.params("name").replaceFirst("\\.rb$", "");
        renderParameterExtractor();
    }
  }

  private void renderParameterExtractor() throws ProjectNotFoundException {
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
          Html.a(ExecutablesComponent.getUrl(project), "Simulators"),
          Html.a(ExecutableComponent.getUrl(executable), executable.getName()),
          "Result Collector",
          collectorName
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getUrl(executable, collectorName, Mode.Update), Html.Method.Post,
          Lte.card(Html.fasIcon("tasks") + "Properties", null,
            Html.div(null,
              Lte.formInputGroup("text", "name", "Name", "Name", collectorName, errors),
              Lte.formDataEditorGroup("collect_script", "Script", "ruby", executable.getCollectorScript(collectorName), errors)
              )
            , Lte.formSubmitButton("primary", "Update")
          )
        );

        content += Html.form(getUrl(executable, collectorName, Mode.Remove), Html.Method.Get,
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
          Html.a(ExecutablesComponent.getUrl(project), "Simulators"),
          Html.a(ExecutableComponent.getUrl(executable), executable.getName()),
          "Result Collector",
          "Add"
        ));
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        content += Html.form(getStaticUrl(executable,  Mode.Add), Html.Method.Post,
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
    String name = request.queryParams("name").replaceFirst("\\.rb$", "");
    executable.createCollector(name);
    response.redirect(getUrl(executable, name));
  }

  public void updateResultCollector() {
    String name = request.queryParams("name").replaceFirst("\\.rb$", "");
    String script = request.queryParams("collect_script");
    executable.updateCollectorScript(name, script);
    response.redirect(getUrl(executable, collectorName));
  }

  public void removeResultCollector() {
    executable.removeCollector(collectorName);
    response.redirect(ExecutableComponent.getUrl(executable));
  }

  public enum Mode {Default, Add, Update, Remove}
}
