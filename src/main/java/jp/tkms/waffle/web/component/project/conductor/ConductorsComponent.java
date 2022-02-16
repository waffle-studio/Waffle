package jp.tkms.waffle.web.component.project.conductor;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.util.FileName;
import jp.tkms.waffle.exception.ChildProcedureNotFoundException;
import jp.tkms.waffle.exception.InvalidInputException;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.ResponseBuilder;
import jp.tkms.waffle.web.component.log.LogsComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.workspace.WorkspaceComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import spark.Spark;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static jp.tkms.waffle.web.template.Html.p;
import static jp.tkms.waffle.web.template.Html.value;

public class ConductorsComponent extends AbstractAccessControlledComponent {
  public static final String TITLE = "Conductor";
  public static final String CONDUCTORS = "Conductors";
  private static final String KEY_MAIN_SCRIPT = "main_script";
  private static final String KEY_LISTENER_SCRIPT = "listener_script";
  private static final String KEY_DEFAULT_VARIABLES = "default_variables";
  private static final String KEY_LISTENER_NAME = "listener_name";
  public static final String KEY_CONDUCTOR = "conductor";
  public static final String KEY_NOTE = "note";
  private static final String NEW_WORKSPACE = "[Create new workspace]";

  public enum Mode {Default, List, Prepare, Run, UpdateArguments, UpdateMainScript, UpdateListenerScript, NewChildProcedure, RemoveConductor, RemoveProcedure, UpdateNote}

  private Mode mode;
  protected Project project;
  //protected Conductor conductor;
  //private ActorRun parent;
  //private SimulatorRun baseRun;
  public ConductorsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public ConductorsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new ResponseBuilder(() -> new ConductorsComponent(Mode.List)));

    ConductorComponent.register();
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + Conductor.CONDUCTOR;
  }

  public static String getAnchorLink(Project project) {
    return Html.a(getUrl(project), CONDUCTORS);
  }

  public static String getUrl(Project project, Mode mode) {
    return getUrl(project) + "/@" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));

    switch (mode){
      default:
        renderConductors();
    }
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
          ProjectsComponent.getAnchorLink(),
          ProjectComponent.getAnchorLink(project),
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
        ArrayList<Lte.TableValue> headers = new ArrayList<>();
        headers.add(new Lte.TableValue("", "Name"));
        headers.add(new Lte.TableValue("", "Note"));
        headers.add(new Lte.TableValue("", ""));

        String content = Lte.card(null, null,
          getConductorsTable(project, headers), null, "card-warning card-outline", "p-0");
        return content;
      }
    }.render(this);
  }

  public static String getConductorsTable(Project project, ArrayList<Lte.TableValue> headers) {
    return Lte.table("table-nooverflow", new Lte.Table() {
      @Override
      public ArrayList<Lte.TableValue> tableHeaders() {
        return headers;
      }

      @Override
      public ArrayList<Future<Lte.TableRow>> tableRows() {
        ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
        for (Conductor conductor : Conductor.getList(project)) {
          list.add(Main.interfaceThreadPool.submit(() -> {
              Lte.TableRow row = new Lte.TableRow(
                ConductorComponent.getAnchorLink(conductor),
                Html.sanitaize(conductor.getNote())
              );
              row.add(new Lte.TableValue("text-align:right;",
                Html.a(ConductorComponent.getUrl(conductor, ConductorComponent.Mode.Prepare),
                  Html.span("right badge badge-secondary", null, "RUN")
                )
              ));
              return row;
            }
          ));
        }
        return list;
      }
    });
  }
}
