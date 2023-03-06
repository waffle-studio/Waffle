package jp.tkms.waffle.data.internal.task;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.project.workspace.run.ProcedureRun;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.inspector.InspectorMaster;
import jp.tkms.waffle.web.component.websocket.PushNotifier;
import jp.tkms.waffle.web.updater.RunStatusUpdater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ExecutableRunTask extends AbstractTask {

  public ExecutableRunTask(Path path, Computer computer) {
    this(WaffleId.newId(), path, computer.getName());
  }

  public ExecutableRunTask(WaffleId id, Path path, String computerName) {
    super(id, path, computerName);
  }

  @Override
  public byte getTypeCode() {
    return ExecutableRunTaskStore.TYPE_CODE;
  }

  public static ExecutableRunTask getInstance(String idHexCode) {
    return InspectorMaster.getExecutableRunTask(WaffleId.valueOf(idHexCode));
  }

  public static ArrayList<ExecutableRunTask> getList() {
    return InspectorMaster.getExecutableRunTaskList();
  }

  public static ArrayList<AbstractTask> getList(Computer computer) {
    return InspectorMaster.getExecutableRunTaskList(computer);
  }

  public static boolean hasJob(Computer computer) {
    return getList(computer).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static void addRun(ExecutableRun run) {
    ExecutableRunTask job = new ExecutableRunTask(run.getLocalPath(), run.getComputer());
    run.setTaskId(job.getHexCode());
    InspectorMaster.registerExecutableRunTask(job);
    InfoLogMessage.issue(run, "was added to the queue");
    //BrowserMessage.addMessage("updateJobNum(" + getNum() + ");"); //TODO: make updater
    new RunStatusUpdater(run);
    PushNotifier.sendJobNumMessage(getNum());
  }

  @Override
  public void remove() {
    InspectorMaster.removeExecutableRunTask(getId());
    try {
      Path storePath = getPropertyStorePath();
      if (Files.exists(storePath)) {
        Files.delete(storePath);
      }
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    //BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
    PushNotifier.sendJobNumMessage(getNum());
  }

  @Override
  public void setState(State state) throws RunNotFoundException {
    super.setState(state);
    ExecutableRun run = getRun();
    if (run != null) {
      if (State.Finished.equals(state) || State.Canceled.equals(state)) {
        run.finish(state);
      } else {
        run.setState(state);
      }

      switch (run.getState()) {
        case Failed:
        case Excepted:
          String failedHandleName = run.getFailedHandler();
          if (failedHandleName != null) {
            ProcedureRun failedHandler = ProcedureRun.create(run.getParentConductorRun(), run.getParentConductorRun().getConductor(), failedHandleName);
            failedHandler.run();
          }
      }
    }
  }

  @Override
  public void replaceComputer(Computer computer) throws RunNotFoundException {
    getRun().setActualComputer(computer);
    InspectorMaster.removeExecutableRunTask(getId());
    String jsonString = StringFileUtil.read(getPropertyStorePath());
    remove();
    setComputerName(computer);
    StringFileUtil.write(getPropertyStorePath(), jsonString);
    setState(State.Created);
    InspectorMaster.registerExecutableRunTask(this);
  }

  @Override
  public ExecutableRun getRun() throws RunNotFoundException {
    return ExecutableRun.getInstance(getPath().toString());
  }

  @Override
  public Path getPropertyStorePath() {
    return ExecutableRunTaskStore.getDirectoryPath().resolve(getComputerName()).resolve(getId().getId() + Constants.EXT_JSON);
  }
}
