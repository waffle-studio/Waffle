package jp.tkms.waffle.data.internal.step;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.ExecutableRunTaskStore;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.util.WaffleId;
import org.json.JSONObject;

import java.nio.file.Path;

public class ProcedureStep implements PropertyFile {
  public static final String KEY_PATH = "path";
  public static final String KEY_PROJECT = "project";
  public static final String KEY_WORKSPACE = "workspace";
  public static final String KEY_ID = "id";

  private JSONObject cache;

  private WaffleId id;
  private Path path;
  private String projectName;
  private String workspaceName;

  public ProcedureStep(Path path, Workspace workspace) {
    this(WaffleId.newId(), path, workspace.getProject().getName(), workspace.getName());
  }

  public ProcedureStep(WaffleId id, Path path, String projectName, String workspaceName) {
    this.id = id;
    this.path = path;
    this.projectName = projectName;
    this.workspaceName = workspaceName;

    setToProperty(KEY_ID, id.getId());
    setToProperty(KEY_PATH, path.normalize().toString());
    setToProperty(KEY_PROJECT, projectName);
    setToProperty(KEY_WORKSPACE, workspaceName);
  }

  public WaffleId getId() {
    return id;
  }

  public Path getPath() {
    return path;
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
    return ProcedureStepStore.getDirectoryPath().resolve(getProjectName()).resolve(getWorkspaceName()).resolve(getId().getId() + Constants.EXT_JSON);
  }
}
