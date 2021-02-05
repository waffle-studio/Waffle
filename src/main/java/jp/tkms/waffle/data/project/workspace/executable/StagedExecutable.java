package jp.tkms.waffle.data.project.workspace.executable;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.HasArchivedInstance;
import jp.tkms.waffle.data.project.workspace.HasWorkspace;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.util.ChildElementsArrayList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class StagedExecutable extends Executable implements HasWorkspace, HasArchivedInstance<ArchivedExecutable> {
  Workspace workspace;

  public StagedExecutable(Workspace workspace, String name) {
    super(workspace.getProject(), name);
    this.workspace = workspace;
    initialise();
  }

  public static ArrayList<StagedExecutable> getList(Workspace workspace) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(workspace), name -> {
      return getInstance(workspace, name.toString());
    });
  }

  public static StagedExecutable getInstance(Workspace workspace, String name) {
    if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name))) {
      return new StagedExecutable(workspace, name);
    }
    return null;
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
          currentArchived = stagedExecutable.getArchivedInstance();
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
      stagedExecutable.updateFileContents(ARCHIVE_ID, archivedExecutable.getId().getReversedBase36Code());
    }

    return stagedExecutable;
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getDirectoryPath().resolve(EXECUTABLE);
  }

  public static Path getDirectoryPath(Workspace workspace, String name) {
    return getBaseDirectoryPath(workspace).resolve(name);
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public Path getDirectoryPath() {
    return getDirectoryPath(workspace, getName());
  }

  @Override
  public ArchivedExecutable getArchivedInstance() {
    return ArchivedExecutable.getInstance(workspace, getName(), getArchiveId());
  }
}
