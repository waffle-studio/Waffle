package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.Trial;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class TrialsComponent extends AbstractAccessControlledComponent {
  private Mode mode;

  ;
  private Project project;
  private Trial trial;
  public TrialsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public TrialsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new TrialsComponent());
  }

  public static String getUrl(Project project) {
    return "/trials/" + (project == null ? ":project/:id" : project.getId() + "/ROOT");
  }

  public static String getUrl(Project project, Trial trial) {
    return "/trials/" + (project == null ? ":project/:id" : project.getId() + "/" + trial.getId());
  }

  public static String getUrl(Project project, Trial trial, String mode) {
    return getUrl(project, trial) + "/" + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    if (requestedId.equals(Trial.ROOT_NAME)){
      trial = Trial.getRootInstance(project);
    } else {
      trial = Trial.getInstance(project, requestedId);
    }

    renderTrialsList();
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderTrialsList() {
    new MainTemplate() {
      @Override
      protected String pageTitle() {
        return "Trials";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getShortId()),
          "Trials",
          request.params("id")
        ));
      }

      @Override
      protected String pageContent() {
        String contents = "";
        contents += Html.javascript(
          "var runUpdated = function(id) {location.reload();};",
          "var runCreated = function(id) {location.reload();};"
        );

        if (! trial.getResults().isEmpty()) {
          contents += Lte.card(Html.faIcon("poll") + "Results",
            Lte.cardToggleButton(true),
            Lte.divRow(
              Lte.divCol(Lte.DivSize.F12,
                Lte.readonlyTextAreaGroup("", null, 10, trial.getResults().toString(2))
              )
            )
            , null);
        }

        contents += Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:6.5em;", "ID"));
              list.add(new Lte.TableValue("", "Name"));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (Trial trial : Trial.getList(project, TrialsComponent.this.trial)) {
                list.add(new Lte.TableRow(
                  Html.a(getUrl(project, trial), null, null, trial.getShortId()),
                  trial.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0");

        contents += Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:6.5em;", "ID"));
              list.add(new Lte.TableValue("", "Conductor"));
              list.add(new Lte.TableValue("", "Simulator"));
              list.add(new Lte.TableValue("", "Host"));
              list.add(new Lte.TableValue("width:2em;", ""));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (SimulatorRun run : SimulatorRun.getList(project, trial)) {
                list.add(new Lte.TableRow(
                  Html.a(RunComponent.getUrl(project, run), run.getShortId()),
                  run.getConductor().getName(),
                  run.getSimulator().getName(),
                  run.getHost().getName(),
                  Html.spanWithId(run.getId() + "-badge", JobsComponent.getStatusBadge(run))
                ));
              }
              return list;
            }
          })
          , null, null, "p-0");

        return contents;
      }
    }.render(this);
  }

  public enum Mode {Default}
}
