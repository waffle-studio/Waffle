package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ConductorComponent extends AbstractAccessControlledComponent {
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_LISTENER_SCRIPT = "listener_script";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_EXT_RUBY = ".rb";
  private static final String KEY_LISTENER_FILENAME = "listener_filename";
  private static final String KEY_NAME = "Name";
  private Mode mode;

  private Project project;
  private Conductor conductor;
  private ConductorRun parent;
  public ConductorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ConductorComponent());
    Spark.get(getUrl(null, "prepare", null), new ConductorComponent(Mode.Prepare));
    Spark.post(getUrl(null, "run", null), new ConductorComponent(Mode.Run));
    Spark.post(getUrl(null, "update-arguments"), new ConductorComponent(Mode.UpdateArguments));
    Spark.post(getUrl(null, "update-main-script"), new ConductorComponent(Mode.UpdateMainScript));
    Spark.post(getUrl(null, "update-listener-script"), new ConductorComponent(Mode.UpdateListenerScript));
    Spark.post(getUrl(null, "new-listener"), new ConductorComponent(Mode.NewListener));

    SimulatorsComponent.register();
    TrialsComponent.register();
  }

  public static String getUrl(Conductor conductor) {
    return "/conductor/"
      + (conductor == null ? ":project/:id" : conductor.getProject().getId() + '/' + conductor.getId());
  }

  public static String getUrl(Conductor conductor, String mode, ConductorRun parent) {
    return getUrl(conductor) + '/' + mode + '/'
      + (parent == null ? ":parent" : parent.getId());
  }

  public static String getUrl(Conductor conductor, String mode) {
    return getUrl(conductor) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    if (!project.isValid()) {
    }

    conductor = Conductor.getInstance(project, request.params("id"));

    if (mode == Mode.Prepare) {
      if (conductor.checkSyntax()) {
        parent = ConductorRun.getInstance(project, request.params("parent"));
        renderPrepareForm();
      } else {
        response.redirect(getUrl(conductor));
      }
    } else if (mode == Mode.Run) {
      parent = ConductorRun.getInstance(project, request.params("parent"));
      ConductorRun conductorRun = ConductorRun.create(conductor.getProject(), parent, conductor);
      if (request.queryMap().hasKey(KEY_DEFAULT_VARIABLES)) {
        conductorRun.putVariablesByJson(request.queryParams(KEY_DEFAULT_VARIABLES));
      }
      conductorRun.start(true);
      response.redirect(ProjectComponent.getUrl(project));
    } else if (mode == Mode.UpdateArguments) {
      if (request.queryMap().hasKey(KEY_DEFAULT_VARIABLES)) {
        conductor.setDefaultVariables(request.queryParams(KEY_DEFAULT_VARIABLES));
      }
      response.redirect(getUrl(conductor));
    } else if (mode == Mode.UpdateMainScript) {
      if (request.queryMap().hasKey(KEY_MAIN_SCRIPT)) {
        conductor.updateMainScriptContents(request.queryParams(KEY_MAIN_SCRIPT));
      }
      response.redirect(getUrl(conductor));
    } else if (mode == Mode.NewListener) {
      if (request.queryMap().hasKey(KEY_NAME)) {
        conductor.createNewListener(request.queryParams(KEY_NAME));
      }
      response.redirect(getUrl(conductor));
    } else if (mode == Mode.UpdateListenerScript) {
      if (request.queryMap().hasKey(KEY_LISTENER_FILENAME) || request.queryMap().hasKey(KEY_LISTENER_SCRIPT)) {
        conductor.updateFileContents(request.queryParams(KEY_LISTENER_FILENAME), request.queryParams(KEY_LISTENER_SCRIPT));
      }
      response.redirect(getUrl(conductor));
    } else {
      renderConductor();
    }
  }

  private void renderConductor() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return conductor.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Conductors",
          conductor.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        ConductorRun lastConductorRun = ConductorRun.getLastInstance(project, conductor);

        if (lastConductorRun != null && ! lastConductorRun.getErrorNote().equals("")) {
          content += Lte.card(Html.faIcon("exclamation-triangle") + "Error of last run",
            Lte.cardToggleButton(false),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.errorNoticeTextAreaGroup(lastConductorRun.getErrorNote())
              )
            )
            , null, "card-danger", null);
        }

        content += Lte.card(Html.faIcon("terminal") + "Basic",
          Html.a(getUrl(conductor, "prepare", ConductorRun.getRootInstance(project)),
            Html.span("right badge badge-secondary", null, "run")
          ) + Lte.cardToggleButton(true) ,
          Html.div(null,
            Lte.readonlyTextInput("Conductor Directory", conductor.getLocation().toAbsolutePath().toString()),
            Lte.readonlyTextInput("Base Script", conductor.getScriptFileName())
          )
          , null, "collapsed-card", null);

        String defaultVariablesText = conductor.getDefaultVariables().toString(2);

        content +=
          Html.form(getUrl(conductor, "update-arguments"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Default variables",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_DEFAULT_VARIABLES, null, defaultVariablesText.split("\\n").length, defaultVariablesText, null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        String mainScriptSyntaxError = RubyConductor.checkSyntax(conductor.getScriptPath());
        content +=
          Html.form(getUrl(conductor, "update-main-script"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Main Script",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  ("".equals(mainScriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(mainScriptSyntaxError)),
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", conductor.getMainScriptContents(), errors),
                  getGuideHtml()
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        content +=
          Html.form(getUrl(conductor, "new-listener"), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "New Listener",
              Lte.cardToggleButton(true),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_NAME, KEY_NAME, "", "", errors),
                  Lte.formSubmitButton("primary", "Create")
                )
              )
              , null, "collapsed-card", null)
          );

        for (File child : conductor.getLocation().toFile().listFiles()) {
          String fileName = child.getName();
          if (child.isFile() && fileName.startsWith(KEY_LISTENER + "-") && fileName.endsWith(KEY_EXT_RUBY)) {
            String scriptSyntaxError = RubyConductor.checkSyntax(child.toPath());
            content +=
              Html.form(getUrl(conductor, "update-listener-script"), Html.Method.Post,
                Lte.card(Html.faIcon("terminal") + fileName.substring(9, fileName.length() -3) + " (Event Listener)",
                  Lte.cardToggleButton(false),
                  Lte.divRow(
                    Lte.divCol(Lte.DivSize.F12,
                      Html.inputHidden(KEY_LISTENER_FILENAME, fileName),
                      ("".equals(scriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(scriptSyntaxError)),
                      Lte.formDataEditorGroup(KEY_LISTENER_SCRIPT, null, "ruby", conductor.getFileContents(fileName), errors),
                      getGuideHtml()
                    )
                  ),
                  Lte.formSubmitButton("success", "Update"),
                  "collapsed-card.stop", null)
              );
          }
        }

        return content;
      }
    }.render(this);
  }

  private String getGuideHtml() {
    return Html.div(null,
      Html.div(null, "hub.invokeListener(\"&lt;NAME&gt;\")"),
      Html.div(null, "hub.createSimulatorRun(\"&lt;NAME&gt;\", \"&lt;HOST&gt;\")"),
      Html.div(null, "hub.createConductorRun(\"&lt;NAME&gt;\")"),
      Html.div(null, "hub.loadConductorTemplate(\"&lt;NAME&gt;\")"),
      Html.div(null, "hub.loadListenerTemplate(\"&lt;NAME&gt;\")")
    );
  }

  private void renderPrepareForm() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return conductor.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "Prepare";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Conductors",
          conductor.getId()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        content +=
          Html.form(getUrl(conductor, "run", parent), Html.Method.Post,
            Lte.card(Html.faIcon("terminal") + "Default variables",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_DEFAULT_VARIABLES, null, 10, conductor.getDefaultVariables().toString(2), null)
                )
              )
              ,Lte.formSubmitButton("primary", "Run")
            )
          );

        return content;
      }
    }.render(this);
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  public enum Mode {Default, Prepare, Run, UpdateArguments, UpdateMainScript, UpdateListenerScript, NewListener}
}
