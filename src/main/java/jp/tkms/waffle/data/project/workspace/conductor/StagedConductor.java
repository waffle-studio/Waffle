package jp.tkms.waffle.data.project.workspace.conductor;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.conductor.Conductor;
import jp.tkms.waffle.data.project.workspace.HasArchivedInstance;
import jp.tkms.waffle.data.project.workspace.HasWorkspace;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedConductor;
import jp.tkms.waffle.data.util.ChildElementsArrayList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class StagedConductor extends Conductor implements HasWorkspace, HasArchivedInstance<ArchivedConductor> {
  private static final Object stagingLocker = new Object();

  Workspace workspace;

  public StagedConductor(Workspace workspace, String name) {
    super(workspace.getProject(), name);
    this.workspace = workspace;

  }

  public static ArrayList<StagedConductor> getList(Workspace workspace) {
    return new ChildElementsArrayList().getList(getBaseDirectoryPath(workspace), name -> {
      return getInstance(workspace, name.toString());
    });
  }

  public static StagedConductor getInstance(Workspace workspace, String name) {
    if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name))) {
      return new StagedConductor(workspace, name);
    }
    return null;
  }

  public static StagedConductor getInstance(Workspace workspace, Conductor conductor) {
    return getInstance(workspace, conductor, false);
  }

  public static StagedConductor getInstance(Workspace workspace, Conductor conductor, boolean forceStaging) {
    StagedConductor stagedConductor = null;
    String name = conductor.getName();

    synchronized (stagingLocker) {
      if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name))) {
        stagedConductor = new StagedConductor(workspace, name);
      }

      if (conductor != null && (forceStaging || stagedConductor == null)) {
        ArchivedConductor currentArchived = null;
        try {
          if (stagedConductor != null) {
            currentArchived = stagedConductor.getArchivedInstance();
            stagedConductor.deleteDirectory();
          }
          conductor.copyDirectory(getDirectoryPath(workspace, name));
          //Files.deleteIfExists(getDirectoryPath(workspace, name).resolve(Conductor.TESTRUN));
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
          return null;
        }
        stagedConductor = new StagedConductor(workspace, name);
        ArchivedConductor archivedConductor = ArchivedConductor.getInstanceOrCreate(stagedConductor, currentArchived);
        stagedConductor.createNewFile(ARCHIVE_ID);
        stagedConductor.updateFileContents(ARCHIVE_ID, archivedConductor.getId().getReversedBase36Code());
      }
    }

    return stagedConductor;
  }

  public static Path getBaseDirectoryPath(Workspace workspace) {
    return workspace.getPath().resolve(CONDUCTOR);
  }

  public static Path getDirectoryPath(Workspace workspace, String name) {
    return getBaseDirectoryPath(workspace).resolve(name);
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public Path getPath() {
    return getDirectoryPath(workspace, getName());
  }

  @Override
  public ArchivedConductor getArchivedInstance() {
    return ArchivedConductor.getInstance(workspace, getName(), getArchiveId());
  }
}
