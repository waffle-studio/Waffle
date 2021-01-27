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
    return new WaffleId(getFileContents(ARCHIVE_ID));
  }

  public static StagedExecutable getInstance(Workspace workspace, Executable executable) {
    String name = executable.getName();

    if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name))) {
      StagedExecutable stagedExecutable = new StagedExecutable(workspace, name);
      return stagedExecutable;
    }

    if (executable != null) {
      try {
        executable.copyDirectory(getDirectoryPath(workspace, name));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
        return null;
      }
      StagedExecutable stagedExecutable = new StagedExecutable(workspace, name);
      ArchivedExecutable archivedExecutable = ArchivedExecutable.create(stagedExecutable);
      stagedExecutable.createNewFile(ARCHIVE_ID);
      stagedExecutable.updateFileContents(ARCHIVE_ID, archivedExecutable.getId().getHexCode());
      return stagedExecutable;
    }

    return null;
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
