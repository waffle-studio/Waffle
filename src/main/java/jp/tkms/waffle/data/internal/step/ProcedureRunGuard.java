package jp.tkms.waffle.data.internal.step;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.WaffleId;
import org.json.JSONObject;

import java.nio.file.Path;

public class ProcedureRunGuard implements PropertyFile {
  public static final String KEY_PATH = "path";
  public static final String KEY_PROJECT = "project";
  public static final String KEY_WORKSPACE = "workspace";
  public static final String KEY_ID = "id";
  public static final String KEY_TARGET = "target";

  private JSONObject cache;

  private WaffleId id;
  private Path procedureRunPath;
  private String projectName;
  private String workspaceName;
  private String targetRunPath;

  public ProcedureRunGuard(ProcedureRun procedureRun, String targetRunPath) {
    this(WaffleId.newId(), procedureRun.getPath(), procedureRun.getProject().getName(), procedureRun.getWorkspace().getName(), targetRunPath);
  }

  public ProcedureRunGuard(WaffleId id, Path procedureRunPath, String projectName, String workspaceName, String targetRunPath) {
    if (procedureRunPath.isAbsolute()) {
      procedureRunPath = Constants.WORK_DIR.relativize(procedureRunPath);
    }

    this.id = id;
    this.procedureRunPath = procedureRunPath;
    this.projectName = projectName;
    this.workspaceName = workspaceName;
    this.targetRunPath = targetRunPath;

    setToProperty(KEY_ID, id.getId());
    setToProperty(KEY_PATH, procedureRunPath.normalize().toString());
    setToProperty(KEY_PROJECT, projectName);
    setToProperty(KEY_WORKSPACE, workspaceName);
    setToProperty(KEY_TARGET, targetRunPath);
  }

  public WaffleId getId() {
    return id;
  }

  public Path getProcedureRunPath() {
    return Constants.WORK_DIR.resolve(procedureRunPath);
  }

  public ProcedureRun getProcedureRun() {
    return ProcedureRun.getInstance(getWorkspace(), this.procedureRunPath);
  }

  public String getProjectName() {
    return projectName;
  }

  public String getWorkspaceName() {
    return workspaceName;
  }

  public Workspace getWorkspace() {
    Project project = Project.getInstance(getProjectName());
    return Workspace.getInstance(project, getWorkspaceName());
  }

  public String getTargetRunPath() {
    return targetRunPath;
  }

  @Override
  public JSONObject getPropertyStoreCache() {
    return cache;
  }

  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    this.cache = cache;
  }

  @Override
  public Path getPropertyStorePath() {
    return ProcedureRunGuardStore.getDirectoryPath().resolve(getProjectName()).resolve(getWorkspaceName()).resolve(getId().getId() + Constants.EXT_JSON);
  }
}
