package jp.tkms.waffle.component;

import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.*;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class RunsComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Runs";
  private static final String ROOT_NAME = "run";
  private Mode mode;

  private Project project;
  private RunNode runNode;
  public RunsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public RunsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new RunsComponent());
  }

  public static String getUrl(Project project) {
    return "/runs/" + (project == null ? ":project/:id" : project.getId() + "/" + ROOT_NAME);
  }

  public static String getUrl(Project project, RunNode node) {
    return "/runs/" + (project == null ? ":project/:id" : project.getId() + "/" + node.getId());
  }

  public static String getUrl(Project project, RunNode node, String mode) {
    return getUrl(project, node) + "/" + mode;
  }

  @Override
  public void controller() {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    if (requestedId.equals(ROOT_NAME)){
      runNode = RunNode.getRootInstance(project);
    } else {
      runNode = RunNode.getInstance(project, requestedId);
    }

    renderTrialsList();
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderTrialsList() {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return "Runs";
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        ArrayList<String> breadcrumb = new ArrayList<String>(Arrays.asList(
          Html.a(ProjectsComponent.getUrl(), "Projects"),
          Html.a(ProjectComponent.getUrl(project), project.getName())
        ));
        ArrayList<String> runNodeList = new ArrayList<>();
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

        contents += Lte.card(null, null,
          Lte.table("table-condensed table-sm", new Lte.Table() {
            @Override
            public ArrayList<Lte.TableValue> tableHeaders() {
              ArrayList<Lte.TableValue> list = new ArrayList<>();
              list.add(new Lte.TableValue("width:0em;", ""));
              list.add(new Lte.TableValue("", "Name"));
              list.add(new Lte.TableValue("width:2em;", ""));
              return list;
            }

            @Override
            public ArrayList<Lte.TableRow> tableRows() {
              ArrayList<Lte.TableRow> list = new ArrayList<>();
              for (RunNode child : runNode.getList()) {
                if (child instanceof SimulatorRunNode) {
                  list.add(new Lte.TableRow(
                      Html.fasIcon("circle"),
                      Html.a(RunComponent.getUrl(project, SimulatorRun.getInstance(project, child.getId())), null, null, child.getSimpleName()),
                      Html.spanWithId(child.getId() + "-badge", child.getState().getStatusBadge())
                    )
                  );
                } else {
                  list.add(new Lte.TableRow(
                      (child instanceof ParallelRunNode ? Html.fasIcon("plus-circle") : Html.farIcon("circle")),
                      Html.a(getUrl(project, child), null, null, child.getSimpleName()),
                      Html.spanWithId(child.getId() + "-badge", child.getState().getStatusBadge())
                    )
                  );
                }
              }
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

  public enum Mode {Default}
}
