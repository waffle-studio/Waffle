package jp.tkms.waffle.data;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.State;

import java.util.ArrayList;
import java.util.UUID;

public class Job {
  private String id = null;
  private String projectName = null;
  private String computerName = null;
  private String jobId = "";
  private State state = null;
  private int errorCount = 0;
  private Double requiredThread = null;
  private Double requiredMemory = null;

  public Job() {}

  public Job(UUID id, Project project, Computer computer) {
    this.id = id.toString();
    this.projectName = project.getName();
    this.computerName = computer.getName();
  }

  public String getId() {
    return id;
  }

  public UUID getUuid() {
    return UUID.fromString(id);
  }

  public String getShortId() {
    return id.replaceFirst("-.*$", "");
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

  public static void addRun(SimulatorRun run) {
    Job job = new Job(run.getUuid(), run.getProject(), run.getComputer());
    Main.jobStore.register(job);
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
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
    SimulatorRun run = getRun();
    if (run != null) {
      run.setState(state);
    }
  }

  public Double getRequiredThread() {
    if (requiredThread == null) {
      try {
        requiredThread = getRun().getSimulator().getRequiredThread();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredThread;
  }

  public Double getRequiredMemory() {
    if (requiredMemory == null) {
      try {
        requiredMemory = getRun().getSimulator().getRequiredMemory();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredMemory;
  }

  public void replaceHost(Computer computer) throws RunNotFoundException {
    getRun().setActualHost(computer);
    Main.jobStore.remove(id);
    this.computerName = computer.getName();
    Main.jobStore.register(this);
  }

  public void incrementErrorCount() {
    errorCount += 1;
  }

  public Project getProject() {
    return Project.getInstance(projectName);
  }

  public Computer getHost() {
    return Computer.getInstance(computerName);
  }

  public SimulatorRun getRun() throws RunNotFoundException {
    return SimulatorRun.getInstance(getProject(), id);
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
