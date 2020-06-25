package jp.tkms.waffle.component;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.ConductorRun;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.RunNode;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import spark.Spark;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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

  public static String getUrl(Project project, SimulatorRun run) {
    return "/run/" + (project == null ? ":project/:id" : project.getId() + "/" + run.getId());
  }

  public static String getUrl(Project project, SimulatorRun run, String mode) {
    return "/run/" + (project == null ? ":project/:id/" + mode : project.getId() + "/" + run.getId() + "/" + mode);
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    run = SimulatorRun.getInstance(project, requestedId);

    switch (mode) {
      case ReCheck:
        run.recheck();
        response.redirect(RunComponent.getUrl(project, run));
        return;
    }

    renderRun();
  }

  private void renderRun() {
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
              public ArrayList<Lte.TableRow> tableRows() {
                ArrayList<Lte.TableRow> list = new ArrayList<>();
                list.add(new Lte.TableRow("Status", run.getState().getStatusBadge()));
                if (run.getConductor() != null) {
                  list.add(new Lte.TableRow("Conductor", Html.a(ConductorComponent.getUrl(run.getConductor()), run.getConductor().getName())));
                } else {
                  list.add(new Lte.TableRow("Conductor", "No Conductor"));
                }
                list.add(new Lte.TableRow("Simulator", Html.a(SimulatorComponent.getUrl(run.getSimulator()), run.getSimulator().getName())));
                list.add(new Lte.TableRow("Host", (run.getHost() == null ? "NotFound" : Html.a(HostComponent.getUrl(run.getHost()), run.getHost().getName()))) );
                list.add(new Lte.TableRow("Exit status", "" + run.getExitStatus()
                  + (run.getExitStatus() == -2
                  ? Html.a(RunComponent.getUrl(project, run, "recheck"),
                  Lte.badge("secondary", null, "ReCheck")):"")));
                list.add(new Lte.TableRow("Created at", run.getCreatedDateTime().toString()));
                list.add(new Lte.TableRow("Submitted at", run.getSubmittedDateTime().toString()));
                list.add(new Lte.TableRow("Finished at", run.getFinishedDateTime().toString()));
                list.add(new Lte.TableRow("Remote Directory", Lte.readonlyTextInputWithCopyButton(null, run.getRemoteWorkingDirectoryLog(), true)));
                return list;
              }
            })
          , null, null, "p-0");

        content += Lte.card(Html.fasIcon("list-alt") + "Variables",
          Lte.cardToggleButton(true),
          Lte.divRow(
            Lte.divCol(Lte.DivSize.F12,
              Lte.readonlyTextAreaGroup("", null, run.getVariables().toString(2))
            )
          )
          , null, "collapsed-card", null);

        content += Lte.card(Html.fasIcon("list-alt") + "Parameters & Results",
          Lte.cardToggleButton(false),
          Lte.divRow(
            Lte.divCol(Lte.DivSize.F12,
              Lte.readonlyTextAreaGroup("", "Parameters", run.getParameters().toString(2))
            ),
            Lte.divCol(Lte.DivSize.F12,
              Lte.readonlyTextAreaGroup("", "Results", run.getResults().toString(2))
            )
          )
          , null);

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
