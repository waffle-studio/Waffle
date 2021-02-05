package jp.tkms.waffle.web.component.project.conductor;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.log.LogsComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutableComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static jp.tkms.waffle.web.template.Html.value;

public class ConductorComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Conductor";
  public static final String CONDUCTORS = "Conductors";
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_LISTENER_SCRIPT = "listener_script";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER_NAME = "listener_name";
  public static final String KEY_CONDUCTOR = "conductor";
  private static final String NEW_WORKSPACE = "[Create new workspace]";
  private Mode mode;

  public enum Mode {Default, List, Prepare, Run, UpdateArguments, UpdateMainScript, UpdateListenerScript, NewChildProcedure}

  protected Project project;
  protected Conductor conductor;
  //private ActorRun parent;
  //private SimulatorRun baseRun;
  public ConductorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(), new ConductorComponent(Mode.List));
    Spark.get(getUrl(null), new ConductorComponent());
    Spark.get(getUrl(null, Mode.Prepare), new ConductorComponent(Mode.Prepare));
    Spark.post(getUrl(null, Mode.Run), new ConductorComponent(Mode.Run));
    Spark.post(getUrl(null, Mode.UpdateArguments), new ConductorComponent(Mode.UpdateArguments));
    Spark.post(getUrl(null, Mode.UpdateMainScript), new ConductorComponent(Mode.UpdateMainScript));
    Spark.post(getUrl(null, Mode.UpdateListenerScript), new ConductorComponent(Mode.UpdateListenerScript));
    Spark.post(getUrl(null, Mode.NewChildProcedure), new ConductorComponent(Mode.NewChildProcedure));
  }

  public static String getUrl() {
    return ProjectComponent.getUrl(null) + "/" + Conductor.CONDUCTOR;
  }

  public static String getUrl(Conductor conductor) {
    return ProjectComponent.getUrl((conductor == null ? null : conductor.getProject())) + "/" + Conductor.CONDUCTOR + "/"
      + (conductor == null ? ":" + KEY_CONDUCTOR : conductor.getName());
  }

  public static String getUrl(Conductor conductor, Mode mode) {
    return getUrl(conductor) + "/@" + mode.name();
  }

  protected Conductor getConductorEntity() {
    return Conductor.getInstance(project, request.params(KEY_CONDUCTOR));
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));

    if (Mode.List.equals(mode)) {
      renderConductors();
    } else {
      conductor = Conductor.getInstance(project, request.params(KEY_CONDUCTOR));

      if (mode == Mode.Prepare) {
        if (conductor.checkSyntax()) {
        /*
        parent = ActorRun.getInstance(project, request.params("parent"));
        try {
          baseRun = SimulatorRun.getInstance(project, request.params("base"));
        } catch (RunNotFoundException e) {
          baseRun = null;
        }

         */
          renderPrepareForm();
        } else {
          response.redirect(getUrl(conductor));
        }
      } else if (mode == Mode.Run) {

        String workspaceName = "" + request.queryParams(WorkspaceComponent.WORKSPACE);
        String newRunName = "" + request.queryParams(KEY_CONDUCTOR);
        Workspace workspace = null;
        if (NEW_WORKSPACE.equals(workspaceName)) {
          workspace = Workspace.create(project, newRunName);
        } else {
          workspace = Workspace.getInstance(project, workspaceName);
        }

      /*
      parent = ActorRun.getInstance(project, request.params("parent"));
      String newRunNodeName = "" + request.queryParams(KEY_NAME);
      ActorRun actorRun = ActorRun.createActorGroupRun(parent.getRunNode().createInclusiveRunNode(newRunNodeName), parent, conductor);
      if (request.queryMap().hasKey(KEY_DEFAULT_VARIABLES)) {
        actorRun.putVariablesByJson(request.queryParams(KEY_DEFAULT_VARIABLES));
      }
      actorRun.start(null, true);
       */

        if (workspace == null) {
          response.redirect(LogsComponent.getUrl());
        } else {
          ConductorRun conductorRun = ConductorRun.create(workspace, conductor, newRunName);
          conductorRun.start();

          response.redirect(WorkspaceComponent.getUrl(workspace.getProject(), workspace));
        }
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
      } else if (mode == Mode.NewChildProcedure) {
        if (request.queryMap().hasKey(KEY_CONDUCTOR)) {
          conductor.createNewChildProcedure(request.queryParams(KEY_CONDUCTOR));
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
  }


  protected String renderSubTitle() {
    return TITLE;
  }

  protected ArrayList<String> renderPageBreadcrumb() {
    return new ArrayList<String>(Arrays.asList(
      Html.a(ProjectsComponent.getUrl(), "Projects"),
      Html.a(ProjectComponent.getUrl(project), project.getName()),
      "Conductors",
      Html.a(ConductorComponent.getUrl(conductor), conductor.getName())
    ));
  }

  protected String renderTool() {
    return "";
  }

  private void renderConductors() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return CONDUCTORS;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          CONDUCTORS
        ));
      }

      @Override
      protected String pageTool() {
        return Html.a(ProjectComponent.getUrl(project, ProjectComponent.Mode.AddConductor),
          null, null, Lte.badge("primary", null, Html.fasIcon("plus-square") + "NEW")
        );
      }

      @Override
      protected String pageContent() {
        String content = Lte.card(null, null,
          Lte.table(null, new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              return null;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
              for (Conductor conductor : Conductor.getList(project)) {
                int runningCount = 0;
                  /*
                  for (Actor notFinished : notFinishedList) {
                    if (notFinished.getActorGroup() != null && notFinished.getActorGroup().getId().equals(conductor.getId())) {
                      runningCount += 1;
                    }
                  }
                   */

                int finalRunningCount = runningCount;
                list.add(Main.interfaceThreadPool.submit(() -> {
                  return new Lte.TableRow(
                    new Lte.TableValue("",
                      Html.a(ConductorComponent.getUrl(conductor),
                        null, null, conductor.getName()
                      )),
                    new Lte.TableValue("text-align:right;",
                      Html.a(ConductorComponent.getUrl(conductor, ConductorComponent.Mode.Prepare),
                        Html.span("right badge badge-secondary", null, "RUN")
                      )
                      /*,
                      new Lte.TableValue("text-align:right;",
                        Html.span(null, null,
                          Html.span("right badge badge-warning", new Html.Attributes(value("id", "conductor-jobnum-" + conductor.getLocalDirectoryPath().toString())))
                          ,
                          "gfhgfhjg"
                          Html.a(ConductorComponent.getUrl(conductor, "prepare", ActorRun.getRootInstance(project)),
                            Html.span("right badge badge-secondary", null, "run")
                          ),
                          Html.javascript("updateConductorJobNum('" + conductor.getLocalDirectoryPath().toString() + "'," + finalRunningCount + ")")
                        )
                      )*/)
                  );
                } ));
              }
              return list;
            }
          })
          , null, "card-warning card-outline", "p-0");
        return content;
      }
    }.render(this);
  }

  private void renderConductor() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return conductor.getName();
      }

      @Override
      protected String pageSubTitle() {
        return TITLE;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          CONDUCTORS
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
            Html.span("right badge badge-warning", new Html.Attributes(value("id", "actorGroup-jobnum-" + conductor.getName()))),
            Html.a(getUrl(conductor, Mode.Prepare),
              Html.span("right badge badge-secondary", null, "RUN")
            ),
            null,
            Html.javascript("updateConductorJobNum('" + conductor.getName() + "'," + runningCount + ")")
          ),
          Html.div(null,
            Lte.readonlyTextInputWithCopyButton("Conductor Directory", conductor.getDirectoryPath().toAbsolutePath().toString())
          )
          , null, "collapsed-card.stop", null);

        content +=
          Html.form(getUrl(conductor, Mode.UpdateArguments), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "Default Variables",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formJsonEditorGroup(KEY_DEFAULT_VARIABLES, null, "tree", conductor.getDefaultVariables().toString(), null)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        String mainScriptSyntaxError = ScriptProcessor.getProcessor(conductor.getMainProcedureScriptPath()).checkSyntax(conductor.getMainProcedureScriptPath());
        content +=
          Html.form(getUrl(conductor, Mode.UpdateMainScript), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "# (Main Script)",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  ("".equals(mainScriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(mainScriptSyntaxError)),
                  Lte.formDataEditorGroup(KEY_MAIN_SCRIPT, null, "ruby", conductor.getMainProcedureScript(), errors),
                  getGuideHtml()
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        content +=
          Html.form(getUrl(conductor, Mode.NewChildProcedure), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + "New Procedure",
              Lte.cardToggleButton(true),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_CONDUCTOR, KEY_CONDUCTOR, "", "", errors)
                )
              )
              , Lte.formSubmitButton("primary", "Create")
              , "collapsed-card", null)
          );

        for (String listenerName : conductor.getChildProcedureNameList()) {
          Path path = conductor.getChildProcedureScriptPath(listenerName);
          String scriptSyntaxError = ScriptProcessor.getProcessor(path).checkSyntax(path);
          content +=
            Html.form(getUrl(conductor, Mode.UpdateListenerScript), Html.Method.Post,
              Lte.card(Html.fasIcon("terminal") + listenerName + " (Event Listener)",
                Lte.cardToggleButton(false),
                Lte.divRow(
                  Lte.divCol(Lte.DivSize.F12,
                    Html.inputHidden(KEY_LISTENER_NAME, listenerName),
                    ("".equals(scriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(scriptSyntaxError)),
                    Lte.formDataEditorGroup(KEY_LISTENER_SCRIPT, null, "ruby", conductor.getChildProcedureScript(listenerName), errors)
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
        Html.div(null, "<b>[RUN(Actor)]</b> = instance.createActorGroupRun(\"&lt;ACTOR_GROUP_NAME&gt;\")"),
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
        return conductor.getName();
      }

      @Override
      protected String pageSubTitle() {
        return renderSubTitle();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return renderPageBreadcrumb();
      }

      @Override
      protected String pageTool() {
        return renderTool();
      }

      @Override
      protected String pageContent() {
        String content = "";

        String name = conductor.getName() + '_' + Main.DATE_FORMAT.format(System.currentTimeMillis());
        String variables = conductor.getDefaultVariables().toString();

        content +=
          Html.form(getUrl(conductor, Mode.Run), Html.Method.Post,
            Lte.card(Html.fasIcon("feather-alt") + "Prepare",
              null,
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formSelectGroup(WorkspaceComponent.WORKSPACE, WorkspaceComponent.WORKSPACE +
                      Html.span("text-danger", null, "(If you select an existing Workspace, the StagedConductor will be replaced even though it is running.)")
                    ,
                    new ArrayList<>() {
                      {
                        add(NEW_WORKSPACE);
                        addAll(Workspace.getList(project).stream().map(workspace -> workspace.getName()).collect(Collectors.toList()));
                      }
                    }
                    , null)
                  ),
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formInputGroup("text", KEY_CONDUCTOR, "Name", "name", FileName.removeRestrictedCharacters(name), null)
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
}
