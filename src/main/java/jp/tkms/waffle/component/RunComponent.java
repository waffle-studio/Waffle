package jp.tkms.waffle.component;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.ActorRun;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.RunNode;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.ProjectNotFoundException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import spark.Spark;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Future;

public class RunComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  ;
  private Project project;
  private SimulatorRun run;
  public RunComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public RunComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null, null), new RunComponent());
    Spark.get(getUrl(null, null, "recheck"), new RunComponent(Mode.ReCheck));
  }

  public static String getUrl(Project project, UUID runId) {
    return "/run/" + (project == null ? ":project/:id" : project.getName() + "/" + runId.toString());
  }

  public static String getUrl(Project project, SimulatorRun run, String mode) {
    return "/run/" + (project == null ? ":project/:id/" + mode : project.getName() + "/" + run.getId() + "/" + mode);
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    try {
      run = SimulatorRun.getInstance(project, requestedId);
    } catch (RunNotFoundException e) {
      return;
    }

    switch (mode) {
      case ReCheck:
        run.recheck();
        response.redirect(RunComponent.getUrl(project, run.getUuid()));
        return;
    }

    renderRun();
  }

  private void renderRun() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return (run.getName() == null || "".equals(run.getName()) ? run.getId() : run.getName());
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName()),
          Html.a(RunsComponent.getUrl(project), "Runs")
        ));
        ArrayList<String> runNodeList = new ArrayList<>();
        RunNode parent = run.getRunNode().getParent();
        while (parent != null) {
          runNodeList.add(Html.a(RunsComponent.getUrl(project, parent), parent.getSimpleName()));
          parent = parent.getParent();
        }
        runNodeList.remove(runNodeList.size() -1);
        Collections.reverse(runNodeList);
        breadcrumb.addAll(runNodeList);
        breadcrumb.add(run.getName());
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        content += Html.javascript("var run_id = '" + run.getId() + "';");

        content += Lte.card(Html.fasIcon("info-circle") + "Status", null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                return null;
              }

              @Override
              public ArrayList<Future<Lte.TableRow>> tableRows() {
                ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Status", run.getState().getStatusBadge());}));
                if (run.getActorGroup() != null) {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Conductor", Html.a(ActorGroupComponent.getUrl(run.getActorGroup()), run.getActorGroup().getName()));}));
                } else {
                  list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Conductor", "No Conductor");}));
                }
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Simulator", Html.a(SimulatorComponent.getUrl(run.getSimulator()), run.getSimulator().getName()));}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Computer", (run.getComputer() == null ? "NotFound" : Html.a(ComputersComponent.getUrl(null, run.getComputer()), run.getComputer().getName())));}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Exit status", "" + run.getExitStatus()
                  + (run.getExitStatus() == -2
                  ? Html.a(RunComponent.getUrl(project, run, "recheck"),
                  Lte.badge("secondary", null, "ReCheck")):""));}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Created at", run.getCreatedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Submitted at", run.getSubmittedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Finished at", run.getFinishedDateTime().toString());}));
                list.add(Main.interfaceThreadPool.submit(() -> { return new Lte.TableRow("Remote Directory", Lte.readonlyTextInputWithCopyButton(null, run.getRemoteWorkingDirectoryLog(), true));}));
                return list;
              }
            })
          , null, null, "p-0");

        if (run.getVariablesStoreSize() <= Constants.HUGE_FILE_SIZE) {
          content += Lte.card(Html.fasIcon("list-alt") + "Variables",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.formJsonEditorGroup("", null, "view", run.getVariables().toString(), null)
              )
            )
            , (run.getActorGroup() == null ? "" : Html.a(ActorGroupComponent.getUrl(run.getActorGroup(), "prepare", ActorRun.getRootInstance(project), run),
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
            , (run.getActorGroup() == null ? "" : Html.a(ActorGroupComponent.getUrl(run.getActorGroup(), "prepare", ActorRun.getRootInstance(project), run),
              Html.span("right badge badge-secondary", null, "run by this variables")
            )), "collapsed-card", null);
        }

        String parametersAndResults = "";

        if (run.getParametersStoreSize() <= Constants.HUGE_FILE_SIZE) {
          parametersAndResults = Lte.divCol(Lte.DivSize.F12,
            Lte.formJsonEditorGroup("", "Parameters", "view", run.getParameters().toString(), null)
          );
        } else {
          parametersAndResults = Lte.divCol(Lte.DivSize.F12,
            Lte.readonlyTextAreaGroup("", "Parameters", "The parameters is too large.")
          );
        }

        if (run.getResultsStoreSize() <= Constants.HUGE_FILE_SIZE) {
          parametersAndResults += Lte.divCol(Lte.DivSize.F12,
            Lte.formJsonEditorGroup("", "Results", "view", run.getResults().toString(), null)
          );
        } else {
          parametersAndResults += Lte.divCol(Lte.DivSize.F12,
            Lte.readonlyTextAreaGroup("", "Results", "The results is too large.")
          );
        }

        content += Lte.card(Html.fasIcon("list-alt") + "Parameters & Results",
          Lte.cardToggleButton(false), Lte.divRow(parametersAndResults) , null);

        if (Files.exists(run.getDirectoryPath().resolve(Constants.STDOUT_FILE))) {
          content += Lte.card(Html.fasIcon("file") + "Standard Output",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, run.getFileContents(Constants.STDOUT_FILE))
              )
            )
            , null, "collapsed-card", null);
        }

        if (Files.exists(run.getDirectoryPath().resolve(Constants.STDERR_FILE))) {
          content += Lte.card(Html.fasIcon("file") + "Standard Error",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, run.getFileContents(Constants.STDERR_FILE))
              )
            )
            , null, "collapsed-card", null);
        }

        return content;
      }
    }.render(this);
  }

  public enum Mode {Default, ReCheck}
}
