package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.ConductorRun;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.SimulatorRun;
import spark.Spark;

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
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return run.getId();
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Trials"
        ));
        ArrayList<String> conductorRunList = new ArrayList<>();
        ConductorRun parent = run.getParent();
        while (parent != null) {
          conductorRunList.add(Html.a(TrialsComponent.getUrl(project, parent), parent.getShortId()));
          parent = parent.getParent();
        }
        Collections.reverse(conductorRunList);
        breadcrumb.addAll(conductorRunList);
        breadcrumb.add(run.getId());
        return breadcrumb;
      }

      @Override
      protected String pageContent() {
        String content = "";

        content += Html.javascript("var run_id = '" + run.getId() + "';");

        content += Lte.card(Html.faIcon("info-circle") + "Status", null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
              @Override
              public ArrayList<Lte.TableValue> tableHeaders() {
                return null;
              }

              @Override
              public ArrayList<Lte.TableRow> tableRows() {
                ArrayList<Lte.TableRow> list = new ArrayList<>();
                list.add(new Lte.TableRow("Status", JobsComponent.getStatusBadge(run)));
                list.add(new Lte.TableRow("Exit status", "" + run.getExitStatus()
                  + (run.getExitStatus() == -2
                  ? Html.a(RunComponent.getUrl(project, run, "recheck"),
                  Lte.badge("secondary", null, "ReCheck")):"")));
                return list;
              }
            })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("list-alt") + "Parameters",
          Lte.cardToggleButton(true),
          Lte.divRow(
            Lte.divCol(Lte.DivSize.F12,
              Lte.readonlyTextAreaGroup("", null, 10, run.getParameters().toString(2))
            )
          )
          , null);

        return content;
      }
    }.render(this);
  }

  public enum Mode {Default, ReCheck}
}
