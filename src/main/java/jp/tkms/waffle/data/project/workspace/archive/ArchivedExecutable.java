package jp.tkms.waffle.data.project.workspace.archive;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.HasWorkspace;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.WaffleId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArchivedExecutable extends Executable implements HasWorkspace, ArchivedEntity {
  private Workspace workspace;
  private WaffleId id;

  public ArchivedExecutable(Workspace workspace, String name, WaffleId id) {
    super(workspace.getProject(), name);
    this.workspace = workspace;
    this.id = id;
    initialise();
  }

  public ArchivedExecutable(Workspace workspace, String name) {
    this(workspace, name, new WaffleId());
  }

  public static ArchivedExecutable create(StagedExecutable stagedExecutable) {
    ArchivedExecutable archivedExecutable = new ArchivedExecutable(stagedExecutable.getWorkspace(), stagedExecutable.getName());
    try {
      stagedExecutable.copyDirectory(archivedExecutable.getPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
      return null;
    }
    return archivedExecutable;
  }

  public static ArchivedExecutable getInstanceOrCreate(StagedExecutable stagedExecutable, ArchivedExecutable comparison) {
    if (stagedExecutable.hasNotDifference(comparison, Paths.get(StagedExecutable.ARCHIVE_ID))) {
      return comparison;
    } else {
      return create(stagedExecutable);
    }
  }

  public static ArchivedExecutable getInstance(Workspace workspace, String name, WaffleId id) {
    if (name != null && !name.equals("") && Files.exists(getDirectoryPath(workspace, name, id))) {
      return new ArchivedExecutable(workspace, name, id);
    }
    return null;
  }

  public static ArchivedExecutable getInstance(Workspace workspace, String name) {
    int separatorIndex = name.lastIndexOf('-');
    return getInstance(workspace, name.substring(0, separatorIndex), WaffleId.valueOf(name.substring(separatorIndex +1)));
  }

  public static Path getDirectoryPath(Workspace workspace, String name, WaffleId id) {
    return workspace.getPath().resolve(Workspace.ARCHIVE).resolve(EXECUTABLE).resolve(ArchivedEntity.getArchiveName(name, id));
  }

  @Override
  public WaffleId getId() {
    return id;
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public Path getPath() {
    return getDirectoryPath(workspace, getName(), id);
  }
}
