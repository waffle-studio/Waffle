package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

public class Job {
  private WaffleId id = null;
  private Path path = null;
  private String computerName = null;
  private String jobId = "";
  private State state = null;
  private int errorCount = 0;
  private Double requiredThread = null;
  private Double requiredMemory = null;

  public Job() {}

  public Job(Path path, Computer computer) {
    this.id = WaffleId.newId();
    this.path = path;
    this.computerName = computer.getName();
  }

  public WaffleId getId() {
    return id;
  }

  public Path getPath() {
    return path;
  }

  public String getHexCode() {
    return id.getReversedHexCode();
  }

  public static Job getInstance(String id) {
    return Main.jobStore.getJob(UUID.fromString(id));
  }

  public static ArrayList<Job> getList() {
    return Main.jobStore.getList();
  }

  public static ArrayList<Job> getList(Computer computer) {
    return new ArrayList<>(Main.jobStore.getList(computer));
  }

  public static boolean hasJob(Computer computer) {
    return getList(computer).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static void addRun(ExecutableRun run) {
    Job job = new Job(run.getLocalDirectoryPath(), run.getComputer());
    Main.jobStore.register(job);
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");"); //TODO: make updater
  }

  public void cancel() throws RunNotFoundException {
    if (getRun().isRunning()) {
      setState(State.Cancel);
    }
  }

  public void remove() {
    Main.jobStore.remove(id);
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
  }

  public void setJobId(String jobId) throws RunNotFoundException {
    this.jobId = jobId;
    getRun().setJobId(jobId);
  }

  public void setState(State state) throws RunNotFoundException {
    this.state = state;
    ExecutableRun run = getRun();
    if (run != null) {
      run.setState(state);
    }
  }

  public Double getRequiredThread() {
    if (requiredThread == null) {
      try {
        requiredThread = getRun().getExecutable().getRequiredThread();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredThread;
  }

  public Double getRequiredMemory() {
    if (requiredMemory == null) {
      try {
        requiredMemory = getRun().getExecutable().getRequiredMemory();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredMemory;
  }

  public void replaceComputer(Computer computer) throws RunNotFoundException {
    getRun().setActualComputer(computer);
    Main.jobStore.remove(id);
    this.computerName = computer.getName();
    Main.jobStore.register(this);
  }

  public void incrementErrorCount() {
    errorCount += 1;
  }

  public Project getProject() throws RunNotFoundException {
    return getRun().getProject();
  }

  public Computer getComputer() {
    return Computer.getInstance(computerName);
  }

  public ExecutableRun getRun() throws RunNotFoundException {
    return ExecutableRun.getInstance(getPath().toString());
  }

  public String getJobId() {
    if ("".equals(jobId)) {
      try {
        jobId = getRun().getJobId();
      } catch (RunNotFoundException e) {
        WarnLogMessage.issue(e);
      }
    }
    return jobId;
  }

  public State getState(boolean disableCache) throws RunNotFoundException {
    if (disableCache || state == null) {
      state = getRun().getState();
    }
    return state;
  }

  public State getState() throws RunNotFoundException {
    return getState(false);
  }

  public int getErrorCount() {
    return errorCount;
  }
}
