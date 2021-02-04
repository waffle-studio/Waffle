package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.web.BrowserMessage;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Job implements PropertyFile {
  private static final String KEY_ERROR_COUNT = "error_count";
  public static final String KEY_PATH = "path";
  public static final String KEY_COMPUTER = "computer";
  public static final String KEY_ID = "id";

  private WaffleId id = null;
  private Path path = null;
  private String computerName = null;
  private String jobId = "";
  private State state = null;
  private Integer errorCount = null;
  private Double requiredThread = null;
  private Double requiredMemory = null;

  public Job(Path path, Computer computer) {
    this(WaffleId.newId(), path, computer.getName());
  }

  public Job(WaffleId id, Path path, String computerName) {
    this.id = id;
    this.path = path;
    this.computerName = computerName;

    setToProperty(KEY_ID, id.getId());
    setToProperty(KEY_PATH, path.normalize().toString());
    setToProperty(KEY_COMPUTER, computerName);
  }

  public WaffleId getId() {
    return id;
  }

  public Path getPath() {
    return path;
  }

  public String getHexCode() {
    return id.getReversedBase36Code();
  }

  public static Job getInstance(String idHexCode) {
    return Main.jobStore.getJob(WaffleId.valueOf(idHexCode));
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

  public boolean exists() {
    return Files.exists(getPropertyStorePath());
  }

  public void remove() {
    Main.jobStore.remove(id);
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
    errorCount = getErrorCount() + 1;
    setToProperty(KEY_ERROR_COUNT, errorCount);
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
    if (errorCount == null) {
      errorCount = getIntFromProperty(KEY_ERROR_COUNT, 0);
    }
    return errorCount;
  }

  @Override
  public Path getPropertyStorePath() {
    return JobStore.getDirectoryPath().resolve(computerName).resolve(id.getId() + Constants.EXT_JSON);
  }

  JSONObject propertyStoreCache = null;
  @Override
  public JSONObject getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(JSONObject cache) {
    propertyStoreCache = cache;
  }
}
