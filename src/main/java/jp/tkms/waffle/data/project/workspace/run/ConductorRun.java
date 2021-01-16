package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;

import java.nio.file.Path;

public class ConductorRun extends AbstractRun {
  public static final String CONDUCTOR_RUN = "CONDUCTOR_RUN";
  public static final String JSON_FILE = CONDUCTOR_RUN + Constants.EXT_JSON;

  public ConductorRun(Workspace workspace, Path path) {
    super(workspace, path);
  }

  @Override
  public Path getPropertyStorePath() {
    return getDirectoryPath().resolve(JSON_FILE);
  }

  public static ConductorRun create(Workspace parent, String expectedName) {
    String name = expectedName;
    ConductorRun instance = new ConductorRun(parent, parent.getDirectoryPath().resolve(name));
    return instance;
  }

  public static ConductorRun create(ConductorRun parent, String expectedName) {
    String name = expectedName;
    ConductorRun instance = new ConductorRun(parent.getWorkspace(), parent.getDirectoryPath().resolve(name));
    return instance;
  }

  static ConductorRun getTestRunConductorRun(ArchivedExecutable executable) {
    return create(executable.getWorkspace(), executable.getName());
  }
}
