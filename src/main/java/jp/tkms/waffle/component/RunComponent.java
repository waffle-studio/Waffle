package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Run;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class RunComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  ;
  private Project project;
  private Run run;
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

  public static String getUrl(Project project, Run run) {
    return "/run/" + (project == null ? ":project/:id" : project.getId() + "/" + run.getId());
  }

  public static String getUrl(Project project, Run run, String mode) {
    return "/run/" + (project == null ? ":project/:id/" + mode : project.getId() + "/" + run.getId() + "/" + mode);
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    run = Run.getInstance(project, requestedId);

    switch (mode) {
      case ReCheck:
        run.start();
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
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Runs",
          request.params("id")
        ));
      }

      @Override
      protected String pageContent() {
        String content = "";

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
                list.add(new Lte.TableRow("Exit status", "" + run.getExitStatus()));
                return list;
              }
            })
          , null, null, "p-0");

        content += Lte.card(Html.faIcon("poll") + "Results",
          Lte.cardToggleButton(true),
          Lte.divRow(
            Lte.divCol(Lte.DivSize.F12,
              Lte.readonlyTextAreaGroup("", null, 10, run.getResults().toString(2))
            )
          )
          , null);


        return content;
      }
    }.render(this);
  }

  public enum Mode {Default, ReCheck}
}
