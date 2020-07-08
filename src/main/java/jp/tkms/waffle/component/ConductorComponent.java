package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.FileName;
import spark.Spark;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static jp.tkms.waffle.component.template.Html.value;

public class ConductorComponent extends AbstractAccessControlledComponent {
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_LISTENER_SCRIPT = "listener_script";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_EXT_RUBY = ".rb";
  private static final String KEY_LISTENER_NAME = "listener_name";
  private static final String KEY_NAME = "Name";
  private Mode mode;

  private Project project;
  private ActorGroup conductor;
  private Actor parent;
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
  }

  public static String getUrl(ActorGroup conductor) {
    return "/conductor/"
      + (conductor == null ? ":project/:id" : conductor.getProject().getId() + '/' + conductor.getId());
  }

  public static String getUrl(ActorGroup conductor, String mode, Actor parent) {
    return getUrl(conductor) + '/' + mode + '/'
      + (parent == null ? ":parent" : parent.getId());
  }

  public static String getUrl(ActorGroup conductor, String mode) {
    return getUrl(conductor) + '/' + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    if (!project.isValid()) {
    }

    conductor = ActorGroup.getInstance(project, request.params("id"));

    if (mode == Mode.Prepare) {
      if (conductor.checkSyntax()) {
        parent = Actor.getInstance(project, request.params("parent"));
        renderPrepareForm();
      } else {
        response.redirect(getUrl(conductor));
      }
    } else if (mode == Mode.Run) {
      parent = Actor.getInstance(project, request.params("parent"));
      String newRunNodeName = "" + request.queryParams(KEY_NAME);
      Actor conductorRun = Actor.create(parent.getRunNode().createInclusiveRunNode(newRunNodeName), parent, conductor);
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
        conductor.updateRepresentativeActorScript(request.queryParams(KEY_MAIN_SCRIPT));
      }
      response.redirect(getUrl(conductor));
    } else if (mode == Mode.NewListener) {
      if (request.queryMap().hasKey(KEY_NAME)) {
        conductor.createNewActor(request.queryParams(KEY_NAME));
      }
      response.redirect(getUrl(conductor));
    } else if (mode == Mode.UpdateListenerScript) {
      if (request.queryMap().hasKey(KEY_LISTENER_NAME) || request.queryMap().hasKey(KEY_LISTENER_SCRIPT)) {
        conductor.updateActorScript(request.queryParams(KEY_LISTENER_NAME), request.queryParams(KEY_LISTENER_SCRIPT));
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
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "Conductors",
          conductor.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        Actor lastConductorRun = Actor.getLastInstance(project, conductor);

        // TODO: do refactoring
        ArrayList<Actor> notFinishedList = new ArrayList<>();
        for (Actor notFinished : Actor.getNotFinishedList(project)) {
          if (!notFinished.isRoot()) {
            if (notFinished.getParentActor() != null && notFinished.getParentActor().isRoot()) {
              notFinishedList.add(notFinished);
            }
          }
        }
        int runningCount = 0;
        for (Actor notFinished : notFinishedList) {
          if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getId().equals(conductor.getId())) {
            runningCount += 1;
          }
        }


        if (lastConductorRun != null && ! lastConductorRun.getErrorNote().equals("")) {
          content += Lte.card(Html.fasIcon("exclamation-triangle") + "Error of last run",
            Lte.cardToggleButton(false),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.errorNoticeTextAreaGroup(lastConductorRun.getErrorNote())
              )
            )
            , null, "card-danger", null);
        }

        content += Html.element("script", new Html.Attributes(value("type", "text/javascript")),
          "var updateConductorJobNum = function(c,n) {" +
            "if (n > 0) {" +
            "document.getElementById('conductor-jobnum-' + c).style.display = 'inline-block';" +
            "document.getElementById('conductor-jobnum-' + c).innerHTML = n;" +
            "} else {" +
            "document.getElementById('conductor-jobnum-' + c).style.display = 'none';" +
            "}" +
            "};"
        );

        content += Lte.card(Html.fasIcon("terminal") + "Properties",
          Html.span(null, null,
            Html.span("right badge badge-warning", new Html.Attributes(value("id", "conductor-jobnum-" + conductor.getId()))),
            Html.a(getUrl(conductor, "prepare", Actor.getRootInstance(project)),
              Html.span("right badge badge-secondary", null, "run")
            ),
            Lte.cardToggleButton(false),
            Html.javascript("updateConductorJobNum('" + conductor.getId() + "'," + runningCount + ")")
          ),
          Html.div(null,
            Lte.readonlyTextInputWithCopyButton("Conductor Directory", conductor.getDirectoryPath().toAbsolutePath().toString())
          )
          , null, "collapsed-card.stop", null);

        content +=
          Html.form(getUrl(conductor, "update-arguments"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Default Variables",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formDataEditorGroup(KEY_DEFAULT_VARIABLES, null, "json", conductor.getDefaultVariables().toString(2), null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        String mainScriptSyntaxError = RubyConductor.checkSyntax(conductor.getRepresentativeActorScriptPath());
        content +=
          Html.form(getUrl(conductor, "update-main-script"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Main Script",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  ("".equals(mainScriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(mainScriptSyntaxError)),
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", conductor.getRepresentativeActorScript(), errors),
                  getGuideHtml()
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        content +=
          Html.form(getUrl(conductor, "new-listener"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "New Listener",
              Lte.cardToggleButton(true),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_NAME, KEY_NAME, "", "", errors)
                )
              )
              , Lte.formSubmitButton("primary", "Create")
              , "collapsed-card", null)
          );

        for (String listenerName : conductor.getActor1NameList()) {
          Path path = conductor.getActorScriptPath(listenerName);
          String scriptSyntaxError = RubyConductor.checkSyntax(path);
          content +=
            Html.form(getUrl(conductor, "update-listener-script"), Html.Method.Post,
              Lte.card(Html.fasIcon("terminal") + listenerName + " (Event Listener)",
                Lte.cardToggleButton(false),
                Lte.divRow(
                  Lte.divCol(Lte.DivSize.F12,
                    Html.inputHidden(KEY_LISTENER_NAME, listenerName),
                    ("".equals(scriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(scriptSyntaxError)),
                    Lte.formDataEditorGroup(KEY_LISTENER_SCRIPT, null, "ruby", conductor.getActorScript(listenerName), errors)
                  )
                ),
                Lte.formSubmitButton("success", "Update"),
                "collapsed-card.stop", null)
            );
        }

        return content;
      }
    }.render(this);
  }

  private String getGuideHtml() {
    return Html.div(null,
      Html.div(null, "<b style='color:green;'>以下，主要コマンド仮表示</b>"),
      Html.element("div", new Html.Attributes(Html.value("style", "padding-left:1em;font-size:0.8em;")),
        Html.div(null, "hub.invokeListener(\"&lt;LISTENER_NAME&gt;\")"),
        Html.div(null, "hub.loadConductorTemplate(\"&lt;NAME&gt;\")"),
        Html.div(null, "hub.loadListenerTemplate(\"&lt;NAME&gt;\")"),
        Html.div(null, "hub.v[:&lt;KEY&gt;] = &lt;VALUE&gt;"),
        Html.div(null, "<b>[RUN(Simulation)]</b> = hub.createSimulatorRun(\"&lt;NAME&gt;\", \"&lt;HOST&gt;\")"),
        Html.div(null, "<b>[RUN(Conductor)]</b> = hub.createConductorRun(\"&lt;NAME&gt;\")"),
        Html.div(null, "<b>[RUN]</b>.addFinalizer(\"&lt;LISTENER_NAME&gt;\")"),
        Html.div(null, "<b>[RUN(Simulation)]</b>.p[:&lt;NAME&gt;] = &lt;VALUE&gt;"),
        Html.div(null, "<b>[RUN(Simulation)]</b>.makeLocalShared(\"&lt;KEY&gt\", \"&lt;REMOTE_FILE&gt\");"),
        Html.div(null, "&lt;VALUE&gt = <b>[RUN(Simulation)]</b>.getResult(\"&lt;VALUE&gt\");")
      )
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
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "Conductors",
          conductor.getName()
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        content +=
          Html.form(getUrl(conductor, "run", parent), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Properties",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_NAME, "Name", "name", FileName.removeRestrictedCharacters(conductor.getName() + '_' + LocalDateTime.now().toString()), null)
                ),
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formDataEditorGroup(KEY_DEFAULT_VARIABLES, "Variables", "json", conductor.getDefaultVariables().toString(2), null)
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
