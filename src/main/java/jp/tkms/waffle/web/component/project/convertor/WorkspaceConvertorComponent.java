package jp.tkms.waffle.web.component.project.convertor;

import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.convertor.WorkspaceConvertor;
import jp.tkms.waffle.exception.ProjectNotFoundException;
import jp.tkms.waffle.web.component.AbstractAccessControlledComponent;
import jp.tkms.waffle.web.component.project.ProjectComponent;
import jp.tkms.waffle.web.component.project.conductor.ConductorComponent;
import jp.tkms.waffle.web.template.Html;
import spark.Spark;

public class WorkspaceConvertorComponent extends AbstractAccessControlledComponent {
  public static final String WORKSPACE_CONVERTORS = "WorkspaceConvertors";
  public static final String KEY_CONVERTOR = "convertor";

  public enum Mode {Default, List, Prepare, Run, UpdateArguments, UpdateMainScript, UpdateListenerScript, NewChildProcedure, RemoveConductor, RemoveProcedure}

  private Mode mode;

  public WorkspaceConvertorComponent(Mode mode) {
    super();
    this.mode = mode;
  }

  public WorkspaceConvertorComponent() {
    this(Mode.Default);
  }

  static public void register() {
    //Spark.get(getUrl(), new ConductorComponent(ConductorComponent.Mode.List));
    Spark.get(getUrl(null), new ConductorComponent());
    Spark.get(getUrl(null, Mode.Prepare), new WorkspaceConvertorComponent(Mode.Prepare));
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

  }
}
