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

public class ProcedureRun extends AbstractRun {
  public static final String PROCEDURE_RUN = "PROCEDURE_RUN";
  public static final String JSON_FILE = PROCEDURE_RUN + Constants.EXT_JSON;

  public ProcedureRun(Workspace workspace, AbstractRun parent, Path path) {
    super(workspace, parent, path);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ProcedureRun create(AbstractRun parent, String expectedName) {
    String name = expectedName;
    ProcedureRun instance = new ProcedureRun(parent.getWorkspace(), parent, parent.getDirectoryPath().resolve(name));
    return instance;
  }

  public static ProcedureRun getInstance(Workspace workspace, String localPathString) {
    Path jsonPath = Constants.WORK_DIR.resolve(localPathString).resolve(JSON_FILE);

    if (Files.exists(jsonPath)) {
      try {
        JSONObject jsonObject = new JSONObject(StringFileUtil.read(jsonPath));
        String parentPath = jsonObject.getString(KEY_PARENT_RUN);
        AbstractRun parent = AbstractRun.getInstance(workspace, parentPath);
        return new ProcedureRun(workspace, parent, jsonPath.getParent());
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }

    return null;
  }

  public static ProcedureRun getTestRunProcedureRun(ArchivedExecutable executable) {
    return create(ConductorRun.getTestRunConductorRun(executable), executable.getArchiveName());
  }
}
