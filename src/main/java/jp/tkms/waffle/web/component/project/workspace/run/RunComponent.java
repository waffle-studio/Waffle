package jp.tkms.waffle.web.component.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.*;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.computer.ComputersComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.executable.ExecutableComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import spark.Spark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

public class RunComponent extends AbstractAccessControlledComponent {
  public static final String RUNS = "Runs";

  private Mode mode;

  private Project project;
  private Workspace workspace;
  private AbstractRun abstractRun;
  private ArrayList<AbstractRun> runList = null;

  public enum Mode {Default, ReCheck, UpdateNote, Root}

  public RunComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public RunComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getRootUrl(null), new WorkspaceComponent(WorkspaceComponent.Mode.RedirectToWorkspace));
    Spark.get(getUrl(null), new RunComponent());
    Spark.get(getUrl(null, Mode.ReCheck), new RunComponent(Mode.ReCheck));
    Spark.get(getUrl(null, Mode.UpdateNote), new RunComponent(Mode.UpdateNote));
  }

  public static String getRootUrl(Workspace workspace) {
    if (workspace != null) {
      return '/' + workspace.getLocalDirectoryPath().toString();
    } else {
      return WorkspaceComponent.getUrl(null, null) + "/RUN";
    }
  }

  public static String getUrl(AbstractRun run) {
    if (run != null) {
      return "/" ;//+ run.getLocalDirectoryPath().toString();
    } else {
      return WorkspaceComponent.getUrl(null, null) + "/RUN/*";
    }
  }

  public static String getUrl(AbstractRun run, Mode mode) {
    return getUrl(run) + "/@" + mode.name();
  }

  public static String getUrlFromPath(Path path) {
    return path.toString();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params(ProjectComponent.KEY_PROJECT));
    workspace = Workspace.getInstance(project, request.params(WorkspaceComponent.KEY_WORKSPACE));

    if (Mode.Root.equals(mode)) {
      controllerRuns();
    } else {
      String localPathString = request.uri().substring(1);

      try {
        abstractRun = AbstractRun.getInstance(workspace, localPathString);
      } catch (RunNotFoundException e) {
        return;
      }

      if (abstractRun instanceof ExecutableRun) {
        controllerRun();
      } else {
        controllerRuns();
      }
    }
  }

  private void controllerRuns() throws ProjectNotFoundException {
    switch (mode) {
      case UpdateNote:
        //run.update();
        response.redirect(RunComponent.getUrl(abstractRun));
        return;
    }

    runList = (abstractRun == null ? AbstractRun.getList(workspace) : abstractRun.getList(null));
    if (Paths.get(request.uri()).relativize(Paths.get(request.headers("Referer").replaceFirst("^.*?/PROJECT/", "/PROJECT/"))).toString().contains("..") && runList.size() == 1) {
      response.redirect(RunComponent.getUrl(runList.get(0)));
    }

    renderRuns();
  }

  private void controllerRun() throws ProjectNotFoundException {
    switch (mode) {
      case ReCheck:
        //run.recheck();
        response.redirect(RunComponent.getUrl(abstractRun));
        return;
    }

    renderRun();
  }

  private void renderRuns() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        if (abstractRun != null) {
          return abstractRun.getName();
        }
        return "Root Run";
      }

      @Override
      protected String pageSubTitle() {
        if (abstractRun != null) {
          if (abstractRun instanceof ProcedureRun) {
            return "ProcedureRun";
          }
          return "ConductorRun";
        }
        return super.pageSubTitle();
      }

      @Override
      protected Workspace pageWorkspace() {
        return workspace;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName())
        ));
        ArrayList<String> runNodeList = new ArrayList<>();
        /*
        RunNode parent = runNode.getParent();
        if (parent == null) {
          breadcrumb.add("Runs");
        } else {
          breadcrumb.add(Html.a(RunsComponent.getUrl(project), "Runs"));
          while (parent != null) {
            runNodeList.add(Html.a(RunsComponent.getUrl(project, parent), parent.getSimpleName()));
            parent = parent.getParent();
          }
          runNodeList.remove(runNodeList.size() - 1);
          Collections.reverse(runNodeList);
          breadcrumb.addAll(runNodeList);
          breadcrumb.add(runNode.getSimpleName());
        }

         */

        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String contents = "";
        contents += Html.javascript(
          "var runUpdated = function(id) {location.reload();};",
          "var runCreated = function(id) {location.reload();};"
        );

        /*
        String errorNote = conductorRun.getErrorNote();
        if (! "".equals(errorNote)) {
          contents += Lte.card(Html.faIcon("exclamation-triangle") + "Error",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.errorNoticeTextAreaGroup(errorNote)
              )
            )
            , null, "card-danger", null);
        }

        if (! conductorRun.getVariables().isEmpty()) {
          contents += Lte.card(Html.faIcon("poll") + "Variables",
            Lte.cardToggleButton(false),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, conductorRun.getVariables().toString(2))
              )
            )
            , null);
        }
         */

        /*
        String note = runNode.getNote();
        contents +=
          Html.form(getUrl( project, runNode, Mode.UpdateNote), Html.Method.Post,
            Lte.card(Html.fasIcon("sticky-note") + "Note",
              Lte.cardToggleButton("".equals(note)),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  Lte.formTextAreaGroup(KEY_NOTE, null, note, null)
                )
              )
              ,Lte.formSubmitButton("success", "Update")
              , ("".equals(note) ? "collapsed-card" : null), null)
          );

         */

        contents += Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:0;", ""));
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("width:50%;", "Note"));
              list.add(new Lte.TableValue("width:0;", ""));
              return list;
            }

            @Override
            public ArrayList<Future<Lte.TableRow>> tableRows() {
              ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();

              for (AbstractRun run : runList) {
                list.add(Main.interfaceThreadPool.submit(() -> {
                    String icon = Html.farIcon("circle");
                    if (run instanceof ProcedureRun) {

                    } else if (run instanceof ConductorRun) {

                    }

                    return new Lte.TableRow(
                      //new Lte.TableValue(null, (child instanceof ParallelRunNode ? Html.fasIcon("plus-circle") : Html.farIcon("circle"))),
                      new Lte.TableValue(null, icon),
                      new Lte.TableValue(null, Html.a(getUrl(run), null, null, run.getName())),
                      new Lte.TableValue("max-width:0;", Html.div("hide-overflow", /*run.getNote()*/ "note")),
                      new Lte.TableValue(null, Html.spanWithId(/*run.getLocalDirectoryPath().toString() +*/ "-badge", run.getState().getStatusBadge()))
                    );
                  })
                );
              }

              /*
              for (RunNode child : runNode.getList()) {
                if (child instanceof SimulatorRunNode) {
                  list.add(Main.interfaceThreadPool.submit(() ->{
                      return new Lte.TableRow(
                        new Lte.TableValue(null, Html.fasIcon("circle")),
                        new Lte.TableValue(null, Html.a(RunComponent.getUrlFromPath(child.getLocalDirectoryPath()), null, null, child.getSimpleName())),
                        new Lte.TableValue("max-width:0;", Html.div("hide-overflow", child.getNote())),
                        new Lte.TableValue(null, Html.spanWithId(child.getId() + "-badge", child.getState().getStatusBadge()))
                      );
                    })
                  );
                } else {
                  list.add(Main.interfaceThreadPool.submit(() -> {
                      return new Lte.TableRow(
                        new Lte.TableValue(null, (child instanceof ParallelRunNode ? Html.fasIcon("plus-circle") : Html.farIcon("circle"))),
                        new Lte.TableValue(null, Html.a(getUrl(project, child), null, null, child.getSimpleName())),
                        new Lte.TableValue("max-width:0;", Html.div("hide-overflow", child.getNote())),
                        new Lte.TableValue(null, Html.spanWithId(child.getId() + "-badge", child.getState().getStatusBadge()))
                      );
                    })
                  );
                }
              }

               */
              return list;
            }
          })
          , null, null, "p-0");

        /*
        contents += Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:6.5em;", "ID"));
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("", "Simulator"));
              list.add(new Lte.TableValue("", "Host"));
              list.add(new Lte.TableValue("width:2em;", ""));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (SimulatorRun run : SimulatorRun.getList(project, conductorRun)) {
                list.add(new Lte.TableRow(
                  Html.a(RunComponent.getUrl(project, run), run.getShortId()),
                  run.getName(),
                  run.getSimulator().getName(),
                  (run.getHost() == null ? "NotFound" : Html.a(HostComponent.getUrl(run.getHost()), run.getHost().getName())),
                  Html.spanWithId(run.getId() + "-badge", run.getState().getStatusBadge())
                ));
              }
              return list;
            }
          })
          , null, null, "p-0");
         */

        return contents;
      }
    }.render(this);
  }

  private void renderRun() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return abstractRun.getName();
      }

      @Override
      protected String pageSubTitle() {
        return "ExecutableRun";
      }

      @Override
      protected Workspace pageWorkspace() {
        return workspace;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), ProjectComponent.PROJECTS),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(WorkspaceComponent.getUrl(project), WorkspaceComponent.WORKSPACES),
          Html.a(WorkspaceComponent.getUrl(project, workspace), workspace.getName())
        ));
        ArrayList<String> runNodeList = new ArrayList<>();
        /*
        RunNode parent = run.getRunNode().getParent();
        while (parent != null) {
          runNodeList.add(Html.a(RunsComponent.getUrl(project, parent), parent.getSimpleName()));
          parent = parent.getParent();
        }
        runNodeList.remove(runNodeList.size() -1);

         */
        Collections.reverse(runNodeList);
        breadcrumb.addAll(runNodeList);
        //breadcrumb.add(run.getName());
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        ExecutableRun executableRun = (ExecutableRun) abstractRun;

        String content = "";

        content += Html.javascript("var run_id = '" + executableRun.getLocalDirectoryPath().toString() + "';");

        content += Lte.card(Html.fasIcon("info-circle") + "Properties", null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                return null;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Status", executableRun.getState().getStatusBadge());}));
                /*
                if (run.getActorGroup() != null) {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Conductor", Html.a(ConductorComponent.getUrl(run.getActorGroup()), run.getActorGroup().getName()));}));
                } else {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Conductor", "No Conductor");}));
                }

                 */
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Executable", Html.a(ExecutableComponent.getUrl(executableRun.getExecutable()), executableRun.getExecutable().getName()));}));
                if (executableRun.getComputer() == executableRun.getActualComputer()) {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Computer",
                    (executableRun.getComputer() == null ? "NotFound" : Html.a(ComputersComponent.getUrl(executableRun.getComputer()), executableRun.getComputer().getName())));}));
                } else {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Computer",
                    (executableRun.getActualComputer() == null ? "NotFound" : Html.a(ComputersComponent.getUrl(executableRun.getActualComputer()), executableRun.getActualComputer().getName())) +
                    (executableRun.getComputer() == null ? "NotFound" : Html.a(ComputersComponent.getUrl(executableRun.getComputer()), "(" + executableRun.getComputer().getName() + ")"))
                  );}));
                }
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Exit status", "" + executableRun.getExitStatus()
                  + (executableRun.getExitStatus() == -2
                  ? Html.a(RunComponent.getUrl(executableRun, Mode.ReCheck),
                  Lte.badge("secondary", null, "ReCheck")):""));}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Created at", executableRun.getCreatedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Submitted at", executableRun.getSubmittedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Finished at", executableRun.getFinishedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Remote Directory", Lte.readonlyTextInputWithCopyButton(null, executableRun.getRemoteWorkingDirectoryLog(), true));}));
                return list;
              }
            })
          , null, null, "p-0");

        /*
        if (run.getVariablesStoreSize() <= Constants.HUGE_FILE_SIZE) {
          content += Lte.card(Html.fasIcon("list-alt") + "Variables",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.formJsonEditorGroup("", null, "view", run.getVariables().toString(), null)
              )
            )
            , (run.getActorGroup() == null ? "" : Html.a(ConductorComponent.getUrl(run.getActorGroup(), "prepare", ActorRun.getRootInstance(project), run),
              Html.span("right badge badge-secondary", null, "run by this variables")
            )), "collapsed-card", null);
        } else {
          content += Lte.card(Html.fasIcon("list-alt") + "Variables",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                "The variables is too large"
              )
            )
            , (run.getActorGroup() == null ? "" : Html.a(ConductorComponent.getUrl(run.getActorGroup(), "prepare", ActorRun.getRootInstance(project), run),
              Html.span("right badge badge-secondary", null, "run by this variables")
            )), "collapsed-card", null);
        }

         */

        String parametersAndResults = "";

        if (executableRun.getParametersStoreSize() <= Constants.HUGE_FILE_SIZE) {
          parametersAndResults = Lte.divCol(Lte.DivSize.F12,
            Lte.formJsonEditorGroup("", "Parameters", "view", executableRun.getParameters().toString(), null)
          );
        } else {
          parametersAndResults = Lte.divCol(Lte.DivSize.F12,
            Lte.readonlyTextAreaGroup("", "Parameters", "The parameters is too large.")
          );
        }

        if (executableRun.getResultsStoreSize() <= Constants.HUGE_FILE_SIZE) {
          parametersAndResults += Lte.divCol(Lte.DivSize.F12,
            Lte.formJsonEditorGroup("", "Results", "view", executableRun.getResults().toString(), null)
          );
        } else {
          parametersAndResults += Lte.divCol(Lte.DivSize.F12,
            Lte.readonlyTextAreaGroup("", "Results", "The results is too large.")
          );
        }

        content += Lte.card(Html.fasIcon("list-alt") + "Parameters & Results",
          Lte.cardToggleButton(false), Lte.divRow(parametersAndResults) , null);

        if (Files.exists(executableRun.getDirectoryPath().resolve(Constants.STDOUT_FILE))) {
          content += Lte.card(Html.fasIcon("file") + "Standard Output",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, executableRun.getFileContents(Constants.STDOUT_FILE))
              )
            )
            , null, "collapsed-card.stop", null);
        }

        if (Files.exists(executableRun.getDirectoryPath().resolve(Constants.STDERR_FILE))) {
          content += Lte.card(Html.fasIcon("file") + "Standard Error",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, executableRun.getFileContents(Constants.STDERR_FILE))
              )
            )
            , null, "collapsed-card.stop", null);
        }

        return content;
      }
    }.render(this);
  }
}
