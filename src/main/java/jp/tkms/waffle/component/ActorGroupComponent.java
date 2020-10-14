package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.conductor.RubyConductor;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.exception.ProjectNotFoundException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.util.FileName;
import spark.Spark;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static jp.tkms.waffle.component.template.Html.value;

public class ActorGroupComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "ActorGroup";
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_LISTENER_SCRIPT = "listener_script";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER = "listener";
  private static final String KEY_EXT_RUBY = ".rb";
  private static final String KEY_LISTENER_NAME = "listener_name";
  private static final String KEY_NAME = "Name";
  private Mode mode;

  private Project project;
  private ActorGroup actorGroup;
  private ActorRun parent;
  private SimulatorRun baseRun;
  public ActorGroupComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ActorGroupComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ActorGroupComponent());
    Spark.get(getUrl(null, "prepare", null), new ActorGroupComponent(Mode.Prepare));
    Spark.get(getUrl(null, "prepare", null, null), new ActorGroupComponent(Mode.Prepare));
    Spark.post(getUrl(null, "run", null), new ActorGroupComponent(Mode.Run));
    Spark.post(getUrl(null, "update-arguments"), new ActorGroupComponent(Mode.UpdateArguments));
    Spark.post(getUrl(null, "update-main-script"), new ActorGroupComponent(Mode.UpdateMainScript));
    Spark.post(getUrl(null, "update-listener-script"), new ActorGroupComponent(Mode.UpdateListenerScript));
    Spark.post(getUrl(null, "new-listener"), new ActorGroupComponent(Mode.NewListener));
  }

  public static String getUrl(ActorGroup conductor) {
    return "/actorGroup/"
      + (conductor == null ? ":project/:id" : conductor.getProject().getName() + '/' + conductor.getName());
  }

  public static String getUrl(ActorGroup conductor, String mode, ActorRun parent) {
    return getUrl(conductor) + '/' + mode + '/'
      + (parent == null ? ":parent" : parent.getId());
  }

  public static String getUrl(ActorGroup conductor, String mode, ActorRun parent, SimulatorRun base) {
    return getUrl(conductor) + '/' + mode
      + '/' + (parent == null ? ":parent" : parent.getId())
      + '/' + (base == null ? ":base" : base.getId());
  }

  public static String getUrl(ActorGroup conductor, String mode) {
    return getUrl(conductor) + '/' + mode;
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));

    actorGroup = ActorGroup.getInstance(project, request.params("id"));

    if (mode == Mode.Prepare) {
      if (actorGroup.checkSyntax()) {
        parent = ActorRun.getInstance(project, request.params("parent"));
        try {
          baseRun = SimulatorRun.getInstance(project, request.params("base"));
        } catch (RunNotFoundException e) {
          baseRun = null;
        }
        renderPrepareForm();
      } else {
        response.redirect(getUrl(actorGroup));
      }
    } else if (mode == Mode.Run) {

      parent = ActorRun.getInstance(project, request.params("parent"));
      String newRunNodeName = "" + request.queryParams(KEY_NAME);
      ActorRun actorRun = ActorRun.create(parent.getRunNode().createInclusiveRunNode(newRunNodeName), parent, actorGroup);
      if (request.queryMap().hasKey(KEY_DEFAULT_VARIABLES)) {
        actorRun.putVariablesByJson(request.queryParams(KEY_DEFAULT_VARIABLES));
      }
      actorRun.start(null, true);
      response.redirect(ProjectComponent.getUrl(project));

    } else if (mode == Mode.UpdateArguments) {
      if (request.queryMap().hasKey(KEY_DEFAULT_VARIABLES)) {
        actorGroup.setDefaultVariables(request.queryParams(KEY_DEFAULT_VARIABLES));
      }
      response.redirect(getUrl(actorGroup));
    } else if (mode == Mode.UpdateMainScript) {
      if (request.queryMap().hasKey(KEY_MAIN_SCRIPT)) {
        actorGroup.updateRepresentativeActorScript(request.queryParams(KEY_MAIN_SCRIPT));
      }
      response.redirect(getUrl(actorGroup));
    } else if (mode == Mode.NewListener) {
      if (request.queryMap().hasKey(KEY_NAME)) {
        actorGroup.createNewActor(request.queryParams(KEY_NAME));
      }
      response.redirect(getUrl(actorGroup));
    } else if (mode == Mode.UpdateListenerScript) {
      if (request.queryMap().hasKey(KEY_LISTENER_NAME) || request.queryMap().hasKey(KEY_LISTENER_SCRIPT)) {
        actorGroup.updateActorScript(request.queryParams(KEY_LISTENER_NAME), request.queryParams(KEY_LISTENER_SCRIPT));
      }
      response.redirect(getUrl(actorGroup));
    } else {
      renderConductor();
    }
  }

  private void renderConductor() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return TITLE;
      }

      @Override
      protected String pageSubTitle() {
        return actorGroup.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "ActorGroups"
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        //Actor lastConductorRun = Actor.getLastInstance(project, actorGroup);

        // TODO: do refactoring
        /*
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
          if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getId().equals(actorGroup.getId())) {
            runningCount += 1;
          }
        }
         */
        int runningCount = 0;


        /*
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

         */

        content += Html.element("script", new Html.Attributes(value("type", "text/javascript")),
          "var updateConductorJobNum = function(c,n) {" +
            "if (n > 0) {" +
            "document.getElementById('actorGroup-jobnum-' + c).style.display = 'inline-block';" +
            "document.getElementById('actorGroup-jobnum-' + c).innerHTML = n;" +
            "} else {" +
            "document.getElementById('actorGroup-jobnum-' + c).style.display = 'none';" +
            "}" +
            "};"
        );

        content += Lte.card(Html.fasIcon("terminal") + "Properties",
          Html.span(null, null,
            Html.span("right badge badge-warning", new Html.Attributes(value("id", "actorGroup-jobnum-" + actorGroup.getName()))),
            Html.a(getUrl(actorGroup, "prepare", ActorRun.getRootInstance(project)),
              Html.span("right badge badge-secondary", null, "run")
            ),
            null,
            Html.javascript("updateConductorJobNum('" + actorGroup.getName() + "'," + runningCount + ")")
          ),
          Html.div(null,
            Lte.readonlyTextInputWithCopyButton("Conductor Directory", actorGroup.getDirectoryPath().toAbsolutePath().toString())
          )
          , null, "collapsed-card.stop", null);

        content +=
          Html.form(getUrl(actorGroup, "update-arguments"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Default Variables",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formJsonEditorGroup(KEY_DEFAULT_VARIABLES, null, "tree", actorGroup.getDefaultVariables().toString(), null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        String mainScriptSyntaxError = RubyConductor.checkSyntax(actorGroup.getRepresentativeActorScriptPath());
        content +=
          Html.form(getUrl(actorGroup, "update-main-script"), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "# (Main Script)",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  ("".equals(mainScriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(mainScriptSyntaxError)),
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", actorGroup.getRepresentativeActorScript(), errors),
                  getGuideHtml()
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        content +=
          Html.form(getUrl(actorGroup, "new-listener"), Html.Method.Post,
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

        for (String listenerName : actorGroup.getActorNameList()) {
          Path path = actorGroup.getActorScriptPath(listenerName);
          String scriptSyntaxError = RubyConductor.checkSyntax(path);
          content +=
            Html.form(getUrl(actorGroup, "update-listener-script"), Html.Method.Post,
              Lte.card(Html.fasIcon("terminal") + listenerName + " (Event Listener)",
                Lte.cardToggleButton(false),
                Lte.divRow(
                  Lte.divCol(Lte.DivSize.F12,
                    Html.inputHidden(KEY_LISTENER_NAME, listenerName),
                    ("".equals(scriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(scriptSyntaxError)),
                    Lte.formDataEditorGroup(KEY_LISTENER_SCRIPT, null, "ruby", actorGroup.getActorScript(listenerName), errors)
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
        //Html.div(null, "hub.invokeListener(\"&lt;LISTENER_NAME&gt;\")"),
        Html.div(null, "instance.loadConductorTemplate(\"&lt;NAME&gt;\")"),
        Html.div(null, "instance.loadListenerTemplate(\"&lt;NAME&gt;\")"),
        Html.div(null, "instance.v[:&lt;KEY&gt;] = &lt;VALUE&gt;"),
        Html.div(null, "<b>[RUN(Simulation)]</b> = instance.createSimulatorRun(\"&lt;SIMULATOR_NAME&gt;\", \"&lt;HOST&gt;\")"),
        Html.div(null, "<b>[RUN(Actor)]</b> = instance.createActorRun(\"&lt;ACTOR_GROUP_NAME&gt;\")"),
        Html.div(null, "<b>[RUN]</b>.addFinalizer(\"&lt;LISTENER_NAME&gt;\")"),
        Html.div(null, "instance.addFinalizer(\"&lt;LISTENER_NAME&gt;\")"),
        Html.div(null, "<b>[RUN(Simulation)]</b>.p[:&lt;NAME&gt;] = &lt;VALUE&gt;"),
        Html.div(null, "<b>[RUN(Simulation)]</b>.makeLocalShared(\"&lt;KEY&gt\", \"&lt;REMOTE_FILE&gt\");"),
        Html.div(null, "&lt;VALUE&gt = <b>[RUN(Simulation)]</b>.getResult(\"&lt;VALUE&gt\");")
      )
    );
  }

  private void renderPrepareForm() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return TITLE;
      }

      @Override
      protected String pageSubTitle() {
        return actorGroup.getName();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          "Conductors",
          Html.a(ActorGroupComponent.getUrl(actorGroup), actorGroup.getName())
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

        String name = actorGroup.getName() + '_' + LocalDateTime.now().toString();
        String variables = actorGroup.getDefaultVariables().toString();

        if (baseRun != null) {
          name = baseRun.getName();
          RunNode parent = baseRun.getRunNode().getParent();
          while (parent != null) {
            name = parent.getSimpleName() + "_" + name;
            parent = parent.getParent();
          }
          name = name + '_' + LocalDateTime.now().toString();
          variables = baseRun.getVariables().toString();
        }

        content +=
          Html.form(getUrl(actorGroup, "run", parent), Html.Method.Post,
            Lte.card(Html.fasIcon("feather-alt") + "Prepare",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_NAME, "Name", "name", FileName.removeRestrictedCharacters(name), null)
                ),
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formJsonEditorGroup(KEY_DEFAULT_VARIABLES, "Variables", "form", variables, null)
                )
              )
              ,Lte.formSubmitButton("primary", "Run"), "card-info", null
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
