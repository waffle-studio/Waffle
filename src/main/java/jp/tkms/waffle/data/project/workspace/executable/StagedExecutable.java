package jp.tkms.waffle.data.project.workspace.executable;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.util.WaffleId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StagedExecutable extends Executable {
  public static final String ARCHIVE_ID = ".ARCHIVE_ID";

  Workspace workspace;

  public StagedExecutable(Workspace workspace, String name) {
    super(workspace.getProject(), name);
    this.workspace = workspace;
    initialise();
  }

  public ArchivedExecutable getEntity() {
    return ArchivedExecutable.getInstance(workspace, getName(), getEntityId());
  }

  public WaffleId getEntityId() {
    return WaffleId.valueOf(getFileContents(ARCHIVE_ID));
  }

  public static StagedExecutable getInstance(Workspace workspace, Executable executable) {
    return getInstance(workspace, executable, false);
  }

  public static StagedExecutable getInstance(Workspace workspace, Executable executable, boolean forceStaging) {
    StagedExecutable stagedExecutable = null;
    String name = executable.getName();

    if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name))) {
      stagedExecutable = new StagedExecutable(workspace, name);
    }

    if (executable != null && (forceStaging || stagedExecutable == null)) {
      ArchivedExecutable currentArchived = null;
      try {
        if (stagedExecutable != null) {
          currentArchived = stagedExecutable.getEntity();
          stagedExecutable.deleteDirectory();
        }
        executable.copyDirectory(getDirectoryPath(workspace, name));
        Files.deleteIfExists(getDirectoryPath(workspace, name).resolve(Executable.TESTRUN));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
        return null;
      }
      stagedExecutable = new StagedExecutable(workspace, name);
      ArchivedExecutable archivedExecutable = ArchivedExecutable.getInstanceOrCreate(stagedExecutable, currentArchived);
      stagedExecutable.createNewFile(ARCHIVE_ID);
      stagedExecutable.updateFileContents(ARCHIVE_ID, archivedExecutable.getId().getReversedHexCode());
    }

    return stagedExecutable;
  }

  public static Path getDirectoryPath(Workspace workspace, String name) {
    return workspace.getDirectoryPath().resolve(EXECUTABLE).resolve(name);
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public Path getDirectoryPath() {
    return getDirectoryPath(workspace, getName());
  }
}
