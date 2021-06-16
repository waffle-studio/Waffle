package jp.tkms.waffle.data.project.workspace.run;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.archive.ArchivedExecutable;
import jp.tkms.waffle.data.project.workspace.executable.StagedExecutable;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class SyncExecutableRun extends ExecutableRun {

  public SyncExecutableRun(Workspace workspace, RunCapsule parent, Path path) {
    super(workspace, parent, path);
  }

  public static SyncExecutableRun create(ProcedureRun parent, String expectedName, ArchivedExecutable executable, Computer computer) {
    RunCapsule capsule = RunCapsule.create(parent, parent.generateUniqueFileName(expectedName));
    String name = capsule.generateUniqueFileName(expectedName);
    SyncExecutableRun run = new SyncExecutableRun(capsule.getWorkspace(), capsule, capsule.getDirectoryPath().resolve(name));
    run.setParent(capsule);
    run.setExecutable(executable);
    run.setComputer(computer);
    run.setActualComputer(computer);
    run.setExpectedName(expectedName);
    run.setState(State.Created);
    run.setToProperty(KEY_EXIT_STATUS, -1);
    run.setToProperty(KEY_CREATED_AT, DateTime.getCurrentEpoch());
    run.setToProperty(KEY_SUBMITTED_AT, DateTime.getEmptyEpoch());
    run.setToProperty(KEY_FINISHED_AT, DateTime.getEmptyEpoch());
    try {
      Files.createDirectories(run.getBasePath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    run.getParent().registerChildRun(run);
    run.updateResponsible();
    run.putParametersByJson(executable.getDefaultParameters().toString());
    return run;
  }

  public static SyncExecutableRun create(ProcedureRun parent, String expectedName, Executable executable, Computer computer) {
    return create(parent, expectedName, StagedExecutable.getInstance(parent.getWorkspace(), executable).getArchivedInstance(), computer);
  }

  @Override
  public void start() {
    super.start();

    while (isRunning()) {
      InfoLogMessage.issue(this, "is waiting to finish");
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }
}
