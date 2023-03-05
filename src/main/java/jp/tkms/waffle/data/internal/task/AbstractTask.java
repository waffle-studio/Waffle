package jp.tkms.waffle.data.internal.task;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.exception.RunNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractTask<T extends ComputerTask> implements PropertyFile {
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

  WrappedJson propertyStoreCache = null;
  @Override
  public WrappedJson getPropertyStoreCache() {
    return propertyStoreCache;
  }
  @Override
  public void setPropertyStoreCache(WrappedJson cache) {
    propertyStoreCache = cache;
  }

  public AbstractTask(WaffleId id, Path path, String computerName) {
    this.id = id;
    this.path = path;
    this.computerName = computerName;

    setToProperty(KEY_ID, id.getId());
    setToProperty(KEY_PATH, path.normalize().toString());
    setToProperty(KEY_COMPUTER, computerName);
  }

  public abstract byte getTypeCode();

  public WaffleId getId() {
    return id;
  }

  public Path getPath() {
    return path;
  }

  public String getHexCode() {
    return id.getReversedBase36Code();
  }

  public void abort() throws RunNotFoundException {
    if (getRun().isRunning()) {
      setState(State.Abort);
    }
  }

  public void cancel() throws RunNotFoundException {
    if (getRun().isRunning()) {
      setState(State.Cancel);
    }
  }

  public boolean exists() {
    return Files.exists(getPropertyStorePath());
  }

  public abstract void remove();

  public void setJobId(String jobId) throws RunNotFoundException {
    this.jobId = jobId;
    getRun().setJobId(jobId);
  }

  public void setState(State state) throws RunNotFoundException {
    this.state = state;
  }

  public Double getRequiredThread() {
    if (requiredThread == null) {
      try {
        requiredThread = getRun().getRequiredThread();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredThread;
  }

  public Double getRequiredMemory() {
    if (requiredMemory == null) {
      try {
        requiredMemory = getRun().getRequiredMemory();
      } catch (RunNotFoundException e) {
        return 0.0;
      }
    }
    return requiredMemory;
  }

  public void setComputerName(Computer computer) {
    this.computerName = computer.getName();
  }

  public String getComputerName() {
    return computerName;
  }

  public abstract void replaceComputer(Computer computer) throws RunNotFoundException;

  public void incrementErrorCount() {
    errorCount = getErrorCount() + 1;
    setToProperty(KEY_ERROR_COUNT, errorCount);
  }

  public Computer getComputer() {
    return Computer.getInstance(computerName);
  }

  public abstract T getRun() throws RunNotFoundException;

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
}
