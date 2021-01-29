package jp.tkms.waffle.data.project.workspace.archive;

import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.WaffleId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ArchivedExecutable extends Executable {
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
      stagedExecutable.copyDirectory(archivedExecutable.getDirectoryPath());
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
    return getInstance(workspace, name.replaceFirst("-.+?$", ""), WaffleId.valueOf(name.replaceFirst("^.+-", "")));
  }

  public static Path getDirectoryPath(Workspace workspace, String name, WaffleId id) {
    return workspace.getDirectoryPath().resolve(Workspace.ARCHIVE).resolve(EXECUTABLE).resolve(getArchiveName(name, id));
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public WaffleId getId() {
    return id;
  }

  public String getArchiveName() {
    return getArchiveName(getName(), id);
  }

  public static String getArchiveName(String name, WaffleId id) {
    return name + "-" + id.getReversedHexCode();
  }

  @Override
  public Path getDirectoryPath() {
    return getDirectoryPath(workspace, getName(), id);
  }
}
