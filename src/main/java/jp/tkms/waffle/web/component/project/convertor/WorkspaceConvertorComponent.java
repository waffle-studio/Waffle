package jp.tkms.waffle.web.component.project.convertor;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.convertor.WorkspaceConvertor;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.script.ScriptProcessor;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.ProjectsComponent;
import jp.tkms.waffle.web.component.project.conductor.ConductorComponent;
import jp.tkms.waffle.web.template.Html;
import jp.tkms.waffle.web.template.Lte;
import jp.tkms.waffle.web.template.ProjectMainTemplate;
import spark.Spark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

public class WorkspaceConvertorComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACE_CONVERTOR = "WorkspaceConvertor";
  public static final String WORKSPACE_CONVERTORS = "WorkspaceConvertors";
  public static final String KEY_CONVERTOR = "convertor";
  public static final String KEY_SCRIPT = "script";
  public static final String KEY_NOTE = "note";

  public enum Mode {Default, Prepare, UpdateNote, Run, UpdateArguments, UpdateScript, UpdateListenerScript, NewChildProcedure, Remove, RemoveProcedure}

  private Mode mode;
  private Project project;
  private WorkspaceConvertor convertor;

  public WorkspaceConvertorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public WorkspaceConvertorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    Spark.get(getUrl(null), new WorkspaceConvertorComponent());
    Spark.get(getUrl(null, Mode.Prepare), new WorkspaceConvertorComponent(Mode.Prepare));
    Spark.post(getUrl(null, Mode.UpdateNote), new WorkspaceConvertorComponent(Mode.UpdateNote));
  }

  public static String getUrl(WorkspaceConvertor convertor) {
    return ProjectComponent.getUrl((convertor == null ? null : convertor.getProject())) + "/" + WorkspaceConvertor.WORKSPACE_CONVERTOR + "/"
      + (convertor == null ? ":" + KEY_CONVERTOR : convertor.getName());
  }

  public static String getAnchorLink(WorkspaceConvertor convertor) {
    return Html.a(getUrl(convertor), convertor.getName());
  }

  public static String getUrl(WorkspaceConvertor convertor, Mode mode) {
    return getUrl(convertor) + "/@" + mode.name();
  }

  @Override
  public void controller() throws ProjectNotFoundException {
    project = Project.getInstance(request.params("project"));
    convertor = WorkspaceConvertor.getInstance(project, request.params(KEY_CONVERTOR));

    switch (mode) {
      case Prepare:
        break;
      case UpdateNote:
        convertor.setNote(request.queryParams(KEY_NOTE));
        response.redirect(getUrl(convertor));
        break;
      default:
        renderConvertor();
    }
  }

  private void renderConvertor() throws ProjectNotFoundException {
    new ProjectMainTemplate(project) {
      @Override
      protected String pageTitle() {
        return WORKSPACE_CONVERTOR;
      }

      @Override
      protected ArrayList<String> pageBreadcrumb() {
        return new ArrayList<String>(Arrays.asList(
          ProjectsComponent.getAnchorLink(),
          ProjectComponent.getAnchorLink(project),
          WORKSPACE_CONVERTORS,
          WorkspaceConvertorComponent.getAnchorLink(convertor)
        ));
      }

      @Override
      protected Workspace pageWorkspace() {
        return null;
      }

      @Override
      protected String pageContent() {
        String contents = "";

        ArrayList<Lte.FormError> errors = new ArrayList<>();

        contents += Html.form(getUrl(convertor, Mode.UpdateNote), Html.Method.Post,
          Lte.card(Html.fasIcon("broom") + convertor.getName(),
          Html.span(null, null,
            //Html.span("right badge badge-warning", new Html.Attributes(value("id", "actorGroup-jobnum-" + conductor.getName()))),
            Html.a(WorkspaceConvertorComponent.getUrl(convertor, Mode.Prepare),
              Html.span("right badge badge-secondary", null, "RUN")
            )
            //, Html.javascript("updateConductorJobNum('" + conductor.getName() + "'," + runningCount + ")")
          ),
          Html.div(null,
            Lte.readonlyTextInputWithCopyButton("WorkspaceConvertor Directory", convertor.getPath().toAbsolutePath().toString()),
            Lte.formTextAreaGroup(KEY_NOTE, "Note", convertor.getNote(), null)
          ), Lte.formSubmitButton("success", "Update"), null, null));

        String snippetScript = "";
        String scriptSyntaxError = ScriptProcessor.getProcessor(convertor.getScriptPath()).checkSyntax(convertor.getScriptPath());
        contents +=
          Html.form(getUrl(convertor, Mode.UpdateScript), Html.Method.Post,
            Lte.card(Html.fasIcon("terminal") + " WorkspaceConvertor Script",
              Lte.cardToggleButton(false),
              Lte.divRow(
                Lte.divCol(Lte.DivSize.F12,
                  ("".equals(scriptSyntaxError) ? null : Lte.errorNoticeTextAreaGroup(scriptSyntaxError)),
                  Lte.formDataEditorGroup(KEY_SCRIPT, null, "ruby", convertor.getScript(), snippetScript, errors)
                )
              ),
              Lte.formSubmitButton("success", "Update"),
              "collapsed-card.stop", null)
          );

        contents += Html.form(getUrl(convertor, Mode.Remove), Html.Method.Get,
          Lte.card(Html.fasIcon("trash-alt") + "Remove",
            Lte.cardToggleButton(true),
            Lte.formInputGroup("text", KEY_CONVERTOR, "Type the name of WorkspaceConvertor.", convertor.getName(), null, null),
            Lte.formSubmitButton("danger", "Remove")
            , "collapsed-card", null
          )
        );

        return contents;
      }
    }.render(this);
  }
}
