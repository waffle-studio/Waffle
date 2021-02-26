package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.StringFileUtil;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ExecutableRunJob extends AbstractJob {

  public ExecutableRunJob(Path path, Computer computer) {
    this(WaffleId.newId(), path, computer.getName());
  }

  public ExecutableRunJob(WaffleId id, Path path, String computerName) {
    super(id, path, computerName);
  }

  public static ExecutableRunJob getInstance(String idHexCode) {
    return Main.jobStore.getJob(WaffleId.valueOf(idHexCode));
  }

  public static ArrayList<ExecutableRunJob> getList() {
    return Main.jobStore.getList();
  }

  public static ArrayList<AbstractJob> getList(Computer computer) {
    return new ArrayList<>(Main.jobStore.getList(computer));
  }

  public static boolean hasJob(Computer computer) {
    return getList(computer).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static void addRun(ExecutableRun run) {
    ExecutableRunJob job = new ExecutableRunJob(run.getLocalDirectoryPath(), run.getComputer());
    Main.jobStore.register(job);
    InfoLogMessage.issue(run, "was added to the queue");
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");"); //TODO: make updater
  }

  @Override
  public void remove() {
    Main.jobStore.remove(getId());
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
    ExecutableRun run = getRun();
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
    Main.jobStore.remove(getId());
    String jsonString = StringFileUtil.read(getPropertyStorePath());
    remove();
    setComputerName(computer);
    setState(State.Created);
    StringFileUtil.write(getPropertyStorePath(), jsonString);
    Main.jobStore.register(this);
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
