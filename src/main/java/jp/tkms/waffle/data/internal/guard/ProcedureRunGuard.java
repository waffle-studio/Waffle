package jp.tkms.waffle.data.internal.guard;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.util.WrappedJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcedureRunGuard implements PropertyFile {
  public static final String KEY_ORIGINAL = "original";
  public static final String KEY_PATH = "path";
  public static final String KEY_PROJECT = "project";
  public static final String KEY_WORKSPACE = "workspace";
  public static final String KEY_ID = "id";
  public static final String KEY_TARGET = "target";
  public static final String KEY_VALUE_GUARD = "value_guard";

  private WrappedJson cache;

  private String originalDescription;
  private WaffleId id;
  private Path procedureRunPath;
  private String projectName;
  private String workspaceName;
  private String targetRunPath;
  private boolean isValueGuard;

  public ProcedureRunGuard(String originalDescription, WaffleId id, Path procedureRunPath, String projectName, String workspaceName, String targetRunPath, boolean isValueGuard) {
    if (procedureRunPath.isAbsolute()) {
      procedureRunPath = Constants.WORK_DIR.relativize(procedureRunPath);
    }

    this.originalDescription = originalDescription;
    this.id = id;
    this.procedureRunPath = procedureRunPath;
    this.projectName = projectName;
    this.workspaceName = workspaceName;
    this.targetRunPath = targetRunPath;
    this.isValueGuard = isValueGuard;

    setToProperty(KEY_ORIGINAL, originalDescription);
    setToProperty(KEY_ID, id.getId());
    setToProperty(KEY_PATH, procedureRunPath.normalize().toString());
    setToProperty(KEY_PROJECT, projectName);
    setToProperty(KEY_WORKSPACE, workspaceName);
    setToProperty(KEY_TARGET, targetRunPath);
    setToProperty(KEY_VALUE_GUARD, isValueGuard);
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

  public String getOriginalDescription() {
    return originalDescription;
  }

  public boolean isValueGuard() {
    return isValueGuard;
  }

  @Override
  public WrappedJson getPropertyStoreCache() {
    return cache;
  }

  @Override
  public void setPropertyStoreCache(WrappedJson cache) {
    this.cache = cache;
  }

  @Override
  public Path getPropertyStorePath() {
    return ProcedureRunGuardStore.getDirectoryPath().resolve(Paths.get(targetRunPath)).resolve(getId().getId() + Constants.EXT_JSON);
  }

  public static ProcedureRunGuard factory(ProcedureRun procedureRun, String guard) {
    String[] slicedGuardString = guard.split(" ", 2);
    ProcedureRunGuard procedureRunGuard = new ProcedureRunGuard(guard, WaffleId.newId(), procedureRun.getPath(), procedureRun.getProject().getName(), procedureRun.getWorkspace().getName(), slicedGuardString[0], (slicedGuardString.length > 1));
    return procedureRunGuard;
  }

  static ProcedureRunGuard factory(Path jsonPath) throws IOException {
    ProcedureRunGuard guard = null;
    try {
      if (jsonPath != null) {
        WrappedJson jsonObject = new WrappedJson(StringFileUtil.read(jsonPath));
        String original = jsonObject.getString(KEY_ORIGINAL, null);
        WaffleId id = WaffleId.valueOf(jsonObject.getLong(KEY_ID, null));
        Path path = Paths.get(jsonObject.getString(KEY_PATH, null));
        String projectName = jsonObject.getString(KEY_PROJECT, null);
        String workspaceName = jsonObject.getString(KEY_WORKSPACE, null);
        String targetRunPath = jsonObject.getString(KEY_TARGET, null);
        boolean isValueGuard = jsonObject.getBoolean(KEY_VALUE_GUARD, null);
        guard = new ProcedureRunGuard(original, id, path, projectName, workspaceName, targetRunPath, isValueGuard);
      }
    } catch (Exception e) {
      throw e;
    }
    return guard;
  }
}
