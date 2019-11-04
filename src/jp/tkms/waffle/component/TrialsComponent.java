package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.MainTemplate;
import jp.tkms.waffle.data.Project;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.data.Trials;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;

public class TrialsComponent extends AbstractComponent {
  private Mode mode;

  ;
  private Project project;
  private Trials trials;
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

  public static String getUrl(Project project, Trials trials) {
    return "/trials/" + (project == null ? ":project/:id" : project.getId() + "/" + trials.getId());
  }

  public static String getUrl(Project project, Trials trials, String mode) {
    return getUrl(project, trials) + "/" + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    if (requestedId.equals(Trials.ROOT_NAME)){
      trials = Trials.getRootInstance(project);
    } else {
      trials = Trials.getInstance(project, requestedId);
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
        return
          Html.javascript(
            "var runUpdated = function(id) {location.reload();};",
            "var runCreated = function(id) {location.reload();};"
          )
            +
          Lte.card(null, null,
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
              for (Trials trials : Trials.getList(project, trials)) {
                list.add(new Lte.TableRow(
                  Html.a(getUrl(project, trials), null, null, trials.getShortId()),
                  trials.getName())
                );
              }
              return list;
            }
          })
          , null, null, "p-0")
          +
          Lte.card(null, null,
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
                for (Run run : Run.getList(project, trials)) {
                  list.add(new Lte.TableRow(
                    run.getShortId(),
                    run.getConductor().getName(),
                    run.getSimulator().getName(),
                    run.getHost().getName(),
                    JobsComponent.getStatusBadge(run)
                  ));
                }
                return list;
              }
            })
          , null, null, "p-0");
      }
    }.render(this);
  }

  public enum Mode {Default}
}
