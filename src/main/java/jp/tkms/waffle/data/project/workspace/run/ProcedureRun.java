package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;

import java.nio.file.Path;

public class ProcedureRun extends AbstractRun {
  public static final String PROCEDURE_RUN = "PROCEDURE_RUN";
  public static final String JSON_FILE = PROCEDURE_RUN + Constants.EXT_JSON;

  public ProcedureRun(Workspace workspace, Path path) {
    super(workspace, path);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ProcedureRun create(ConductorRun parent, String expectedName) {
    String name = expectedName;
    ProcedureRun instance = new ProcedureRun(parent.getWorkspace(), parent.getDirectoryPath().resolve(name));
    return instance;
  }

  public static ProcedureRun getTestRunProcedureRun(ArchivedExecutable executable) {
    return create(ConductorRun.getTestRunConductorRun(executable), executable.getArchiveName());
  }
}
