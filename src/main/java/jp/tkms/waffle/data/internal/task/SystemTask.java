package jp.tkms.waffle.data.internal.task;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.SystemTaskRun;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.inspector.InspectorMaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class SystemTask extends AbstractTask {
  public SystemTask(Path path, Computer computer) {
    this(WaffleId.newId(), path, computer.getName());
  }

  public SystemTask(WaffleId id, Path path, String computerName) {
    super(id, path, computerName);
  }

  @Override
  public byte getTypeCode() {
    return SystemTaskStore.TYPE_CODE;
  }

  public static SystemTask getInstance(String idHexCode) {
    return InspectorMaster.getSystemTask(WaffleId.valueOf(idHexCode));
  }

  public static ArrayList<SystemTask> getList() {
    return InspectorMaster.getSystemTaskList();
  }

  public static ArrayList<AbstractTask> getList(Computer computer) {
    return InspectorMaster.getSystemTaskList(computer);
  }

  public static boolean hasJob(Computer computer) {
    return getList(computer).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static SystemTask addRun(SystemTaskRun run) {
    SystemTask job = new SystemTask(run.getLocalPath(), run.getComputer());
    InspectorMaster.registerSystemTask(job);
    InfoLogMessage.issue(run, "was added to the queue");
    //System.out.println(job.getPropertyStorePath().toString());
    //BrowserMessage.addMessage("updateJobNum(" + getNum() + ");"); //TODO: make updater
    return job;
  }

  @Override
  public void setJobId(String jobId) throws RunNotFoundException {
    super.setJobId(jobId);
  }

  @Override
  public void remove() {
    InspectorMaster.removeSystemTask(getId());
    try {
      Path storePath = getPropertyStorePath();
      if (Files.exists(storePath)) {
        Files.delete(storePath);
      }
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    //BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
  }

  @Override
  public void setState(State state) throws RunNotFoundException {
    super.setState(state);
    SystemTaskRun run = getRun();
    if (run != null) {
      switch (state) {
        case Aborted:
        case Excepted:
        case Failed:
        case Finished:
          run.finish();
          run.setState(state);
          break;
        default:
          run.setState(state);
      }
    }
  }

  @Override
  public void replaceComputer(Computer computer) throws RunNotFoundException {
    getRun().setActualComputer(computer);
    String jsonString = StringFileUtil.read(getPropertyStorePath());
    InspectorMaster.removeSystemTask(getId());
    remove();
    setComputerName(computer);
    StringFileUtil.write(getPropertyStorePath(), jsonString);
    setState(State.Created);
    InspectorMaster.registerSystemTask(this);
  }

  @Override
  public SystemTaskRun getRun() throws RunNotFoundException {
    return SystemTaskRun.getInstance(getPath().toString());
  }

  @Override
  public Path getPropertyStorePath() {
    return SystemTaskStore.getDirectoryPath().resolve(getComputerName()).resolve(getId().getId() + Constants.EXT_JSON);
  }
}
