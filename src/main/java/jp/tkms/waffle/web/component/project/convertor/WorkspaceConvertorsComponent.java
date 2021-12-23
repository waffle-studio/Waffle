package jp.tkms.waffle.web.component.project.convertor;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.convertor.WorkspaceConvertor;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

public class WorkspaceConvertorsComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACE_CONVERTOR = "WorkspaceConvertor";
  public static final String WORKSPACE_CONVERTORS = "WorkspaceConvertors";
  public static final String KEY_CONVERTOR = "convertor";
  public static final String KEY_SCRIPT = "script";
  public static final String KEY_NOTE = "note";

  public enum Mode {Default, List, Prepare, UpdateNote, Run, UpdateArguments, UpdateScript, UpdateListenerScript, NewChildProcedure, Remove, RemoveProcedure}

  private Mode mode;
  private Project project;

  public WorkspaceConvertorsComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public WorkspaceConvertorsComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new WorkspaceConvertorsComponent(Mode.List));

    WorkspaceConvertorComponent.register();
  }

  public static String getUrl(Project project) {
    return ProjectComponent.getUrl(project) + "/" + WorkspaceConvertor.WORKSPACE_CONVERTOR;
  }

  public static String getAnchorLink(Project project) {
    return Html.a(getUrl(project), WORKSPACE_CONVERTORS);
  }

  public static String getUrl(Project project, Mode mode) {
    return getUrl(project) + "/@" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));

    switch (mode) {
      default:
        renderConvertors();
    }
  }

  private void renderConvertors() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return WORKSPACE_CONVERTORS;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          ProjectsComponent.getAnchorLink(),
          ProjectComponent.getAnchorLink(project),
          WORKSPACE_CONVERTORS
        ));
      }

      @Override
      protected String pageTool() {
        return Html.a(ProjectComponent.getUrl(project, ProjectComponent.Mode.AddWorkspaceConvertor),
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
          getWorkspaceConvertorsTable(project, headers), null, null, "p-0");
        return content;
      }
    }.render(this);
  }

  public static String getWorkspaceConvertorsTable(Project project, ArrayList<Lte.TableValue> headers) {
    return Lte.table("table-nooverflow", new Lte.Table() {
      @Override
      public ArrayList<Lte.TableValue> tableHeaders() {
        return headers;
      }

      @Override
      public ArrayList<Future<Lte.TableRow>> tableRows() {
        ArrayList<Future<Lte.TableRow>> list = new ArrayList<>();
        for (WorkspaceConvertor convertor : WorkspaceConvertor.getList(project)) {
          list.add(Main.interfaceThreadPool.submit(() -> {
              Lte.TableRow row = new Lte.TableRow(
                WorkspaceConvertorComponent.getAnchorLink(convertor),
                Html.sanitaize(convertor.getNote())
              );
              row.add(new Lte.TableValue("text-align:right;",
                  Html.a(WorkspaceConvertorComponent.getUrl(convertor, WorkspaceConvertorComponent.Mode.Prepare),
                    Html.span("right badge badge-secondary", null, "RUN")
                  )
                )
              );
              return row;
            }
          ));
        }
        return list;
      }
    });
  }
}
