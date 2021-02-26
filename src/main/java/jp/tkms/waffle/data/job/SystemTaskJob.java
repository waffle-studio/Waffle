package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.SystemTaskRun;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.exception.RunNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class SystemTaskJob extends AbstractJob {
  public SystemTaskJob(Path path, Computer computer) {
    this(WaffleId.newId(), path, computer.getName());
  }

  public SystemTaskJob(WaffleId id, Path path, String computerName) {
    super(id, path, computerName);
  }

  public static SystemTaskJob getInstance(String idHexCode) {
    return Main.systemTaskStore.getJob(WaffleId.valueOf(idHexCode));
  }

  public static ArrayList<SystemTaskJob> getList() {
    return Main.systemTaskStore.getList();
  }

  public static ArrayList<AbstractJob> getList(Computer computer) {
    return new ArrayList<>(Main.systemTaskStore.getList(computer));
  }

  public static boolean hasJob(Computer computer) {
    return getList(computer).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static void addRun(SystemTaskRun run) {
    SystemTaskJob job = new SystemTaskJob(run.getLocalDirectoryPath(), run.getComputer());
    Main.systemTaskStore.register(job);
    InfoLogMessage.issue(run, "was added to the queue");
    //System.out.println(job.getPropertyStorePath().toString());
    //BrowserMessage.addMessage("updateJobNum(" + getNum() + ");"); //TODO: make updater
  }

  @Override
  public void setJobId(String jobId) throws RunNotFoundException {
    super.setJobId(jobId);
  }

  @Override
  public void remove() {
    Main.systemTaskStore.remove(getId());
    try {
      Path storePath = getPropertyStorePath();
      if (Files.exists(storePath)) {
        Files.delete(storePath);
      }
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
  }

  @Override
  public void setState(State state) throws RunNotFoundException {
    super.setState(state);
    SystemTaskRun run = getRun();
    if (run != null) {
      switch (state) {
        case Canceled:
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
    Main.systemTaskStore.remove(getId());
    String jsonString = StringFileUtil.read(getPropertyStorePath());
    remove();
    setComputerName(computer);
    setState(State.Created);
    StringFileUtil.write(getPropertyStorePath(), jsonString);
    Main.systemTaskStore.register(this);
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
