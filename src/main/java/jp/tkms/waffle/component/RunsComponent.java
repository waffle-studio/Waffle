package jp.tkms.waffle.component;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.component.template.Html;
import jp.tkms.waffle.component.template.Lte;
import jp.tkms.waffle.component.template.ProjectMainTemplate;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.exception.ProjectNotFoundException;
import jp.tkms.waffle.data.util.State;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;

public class RunsComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Runs";
  private static final String ROOT_NAME = "run";
  private static final String KEY_NOTE = "Note";
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
    Spark.get(getUrl(null, null), new RunsComponent());
    Spark.post(getUrl(null, null, Mode.UpdateNote), new RunsComponent(Mode.UpdateNote));
  }

  public static String getUrl(Project project) {
    return "/runs/" + (project == null ? ":project/:id" : project.getName() + "/" + ROOT_NAME);
  }

  public static String getUrl(Project project, RunNode node) {
    return "/runs/" + (project == null ? ":project/:id" : project.getName() + "/" + node.getId());
  }

  public static String getUrl(Project project, RunNode node, Mode mode) {
    return getUrl(project, node) + "/" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    String requestedId = request.params("id");

    if (requestedId.equals(ROOT_NAME)){
      runNode = RunNode.getRootInstance(project);
    } else {
      runNode = RunNode.getInstance(project, requestedId);
    }

    if (mode.equals(Mode.UpdateNote)) {
      updateNote();
    } else {
      renderRunsList();
    }
  }

  private ArrayList<Lte.FormError> checkCreateProjectFormError() {
    return new ArrayList<>();
  }

  private void renderRunsList() throws ProjectNotFoundException {
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
              for (RunNode child : runNode.getList()) {
                if (child instanceof SimulatorRunNode) {
                  list.add(Main.threadPool.submit(() ->{
                      return new Lte.TableRow(
                        new Lte.TableValue(null, Html.fasIcon("circle")),
                        new Lte.TableValue(null, Html.a(RunComponent.getUrl(project, child.getUuid()), null, null, child.getSimpleName())),
                        new Lte.TableValue("max-width:0;", Html.div("hide-overflow", child.getNote())),
                        new Lte.TableValue(null, Html.spanWithId(child.getId() + "-badge", child.getState().getStatusBadge()))
                      );
                    })
                  );
                } else {
                  list.add(Main.threadPool.submit(() -> {
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

  void updateNote() {
    runNode.setNote(request.queryParamOrDefault(KEY_NOTE, ""));
    response.redirect(getUrl(project, runNode));
  }

  public enum Mode {Default, UpdateNote}
}
