package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.util.PathSemaphore;
import jp.tkms.waffle.data.util.StringFileUtil;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConductorRun extends AbstractRun {
  public static final String CONDUCTOR_RUN = "CONDUCTOR_RUN";
  public static final String JSON_FILE = CONDUCTOR_RUN + Constants.EXT_JSON;

  public ConductorRun(Workspace workspace, AbstractRun parent, Path path) {
    super(workspace, parent, path);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ConductorRun create(Workspace workspace, String expectedName) {
    String name = expectedName;
    ConductorRun instance = new ConductorRun(workspace, null, workspace.getDirectoryPath().resolve(AbstractRun.RUN).resolve(name));
    return instance;
  }

  public static ConductorRun create(ProcedureRun parent, String expectedName) {
    String name = expectedName;
    ConductorRun instance = new ConductorRun(parent.getWorkspace(), parent, parent.getDirectoryPath().resolve(name));
    return instance;
  }

  public static ConductorRun getInstance(Workspace workspace, String localPathString) {
    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        AbstractRun parent = null;
        if (jsonObject.keySet().contains(KEY_PARENT_RUN)) {
          String parentPath = jsonObject.getString(KEY_PARENT_RUN);
          parent = AbstractRun.getInstance(workspace, parentPath);
        }
        return new ConductorRun(workspace, parent, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(jsonPath.toString() + " : " + ErrorLogMessage.getStackTrace(e));
      }
    }

    return null;
  }

  static ConductorRun getTestRunConductorRun(ArchivedExecutable executable) {
    return create(executable.getWorkspace(), executable.getName());
  }
}
