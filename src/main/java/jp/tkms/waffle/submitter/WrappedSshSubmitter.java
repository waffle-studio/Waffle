package jp.tkms.waffle.submitter;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.SystemTaskRun;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.RunNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class WrappedSshSubmitter extends SshSubmitter {
  public static final String JOB_MANAGER = "JOB_MANAGER";
  public static final String BIN = "BIN";
  public static final String JOB_MANAGER_JSON_FILE = JOB_MANAGER + Constants.EXT_JSON;

  JobManager jobManager;

  public WrappedSshSubmitter(Computer computer) {
    super(computer);
    jobManager = new JobManager(computer);
  }

  @Override
  public void submit(AbstractJob job) throws RunNotFoundException {
    try {
      if (job.getState().equals(State.Created)) {
        prepareJob(job);
      }
      String execstr =  exec(xsubSubmitCommand(job));
      processXsubSubmit(job, execstr);
    } catch (FailedToControlRemoteException e) {
      WarnLogMessage.issue(job.getComputer(), e.getMessage());
      job.setState(State.Excepted);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      job.setState(State.Excepted);
    }
  }



  static final String RUNNING = "running";
  static final String ACTIVE = "active";
  static final String RUN_SH_FILE = "./run.sh";
  public static final Path JOBS_PATH = Paths.get("JOBS");
  public static final Path ENTITIES_PATH = Paths.get("ENTITIES");
  public static final Path ALIVE_NOTIFIER_PATH = Paths.get("ALIVE_NOTIFIER");
  public static final Path LOCKOUT_FILE_PATH = ALIVE_NOTIFIER_PATH.resolve("LOCKOUT");
  public class JobManager implements DataDirectory, PropertyFile {
    Computer computer;
    ArrayList<VirtualJobExecutor> runningExecutorList;
    ArrayList<VirtualJobExecutor> activeExecutorList;

    JobManager(Computer computer) {
      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      this.computer = computer;
      if (getArrayFromProperty(RUNNING) == null) {
        putNewArrayToProperty(RUNNING);
      }
      if (getArrayFromProperty(ACTIVE) == null) {
        putNewArrayToProperty(ACTIVE);
      }
      runningExecutorList = new ArrayList<>();
      activeExecutorList = new ArrayList<>();
      JSONArray runningArray = getArrayFromProperty(RUNNING);
      for (int i = 0; i < runningArray.length(); i++) {
        runningExecutorList.add(VirtualJobExecutor.getInstance(this, WaffleId.valueOf(runningArray.getString(i))));
      }
      JSONArray activeArray = getArrayFromProperty(ACTIVE);
      for (int i = 0; i < activeArray.length(); i++) {
        activeExecutorList.add(VirtualJobExecutor.getInstance(this, WaffleId.valueOf(activeArray.getString(i))));
      }
    }

    public VirtualJobExecutor getNextExecutor(double threadSize, double memorySize) {
      VirtualJobExecutor result = null;
      for (VirtualJobExecutor executor : activeExecutorList) {
        if (threadSize <= (getMaximumNumberOfThreads(computer) - executor.getUsedThread())
          && memorySize <= (getAllocableMemorySize(computer) - executor.getUsedMemory())) {
          if (executor.isAcceptable()) {
            result = executor;
            break;
          }
        }
      }
      if (result == null) {
        result = VirtualJobExecutor.create(this, 0, 0);
        registerExecutor(result);
      }
      return result;
    }

    void registerExecutor(VirtualJobExecutor executor) {
      putToArrayOfProperty(RUNNING, executor.getId());
      runningExecutorList.add(executor);
      putToArrayOfProperty(ACTIVE, executor.getId());
      activeExecutorList.add(executor);
    }

    boolean fileExists(Path path) throws FailedToControlRemoteException {
      return exists(path);
    }

    public Computer getComputer() {
      return computer;
    }

    @Override
    public Path getDirectoryPath() {
      return computer.getDirectoryPath().resolve(JOB_MANAGER);
    }

    @Override
    public Path getPropertyStorePath() {
      return getDirectoryPath().resolve(JOB_MANAGER_JSON_FILE);
    }

    JSONObject jsonObject = new JSONObject();
    @Override
    public JSONObject getPropertyStoreCache() {
      return jsonObject;
    }

    @Override
    public void setPropertyStoreCache(JSONObject cache) {
      jsonObject = cache;
    }

    public Path getBinDirectory() {
      Path dirPath = getDirectoryPath().resolve(BIN);
      if (!dirPath.toFile().exists()) {
        try {
          Files.createDirectories(dirPath);
        } catch (IOException e) {
          ErrorLogMessage.issue(e);
        }
      }
      return dirPath;
    }
  }

  static class VirtualJobExecutor extends SystemTaskRun {
    JobManager jobManager;
    WaffleId id;
    ArrayList<ExecutableRunJob> runningList;

    VirtualJobExecutor(JobManager jobManager, WaffleId id) {
      super(getDirectoryPath(jobManager, id));
      this.jobManager = jobManager;
      this.id = id;
      if (getArrayFromProperty(RUNNING) == null) {
        putNewArrayToProperty(RUNNING);
      }
      runningList = new ArrayList<>();
      JSONArray runningArray = getArrayFromProperty(RUNNING);
      for (int i = 0; i < runningArray.length(); i++) {
        WaffleId targetJobId = WaffleId.valueOf(runningArray.getLong(i));
        for (ExecutableRunJob job : Main.jobStore.getList()) {
          if (job.getId().equals(targetJobId)) {
            runningList.add(job);
            break;
          }
        }
      }
    }

    String getId() {
      return id.getReversedBase36Code();
    }

    public static VirtualJobExecutor getInstance(JobManager jobManager, WaffleId id) {
      if (getDirectoryPath(jobManager, id).toFile().exists()) {
        return new VirtualJobExecutor(jobManager, id);
      }
      return null;
    }

    public static VirtualJobExecutor create(JobManager jobManager, int threadSize, int memorySize) {
      VirtualJobExecutor executor = new VirtualJobExecutor(jobManager, WaffleId.newId());
      executor.setCommand(RUN_SH_FILE);
      executor.setBinPath(jobManager.getBinDirectory());
      executor.setRequiredThread(threadSize);
      executor.setRequiredMemory(memorySize);
      executor.setComputer(jobManager.getComputer());
      executor.setState(State.Created);
      executor.setToProperty(KEY_EXIT_STATUS, -1);
      executor.setToProperty(KEY_CREATED_AT, DateTime.getCurrentEpoch());
      executor.setToProperty(KEY_SUBMITTED_AT, DateTime.getEmptyEpoch());
      executor.setToProperty(KEY_FINISHED_AT, DateTime.getEmptyEpoch());
      try {
        Files.createDirectories(executor.getBasePath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      return executor;
    }

    public boolean isAcceptable() {
      switch (getState()) {
        case Created:
        case Prepared:
        case Submitted:
        case Running:
          try {
            if (!jobManager.fileExists(getRemoteLockoutFilePath())) {
              return true;
            }
          } catch (FailedToControlRemoteException e) {
            ErrorLogMessage.issue(e);
          }
      }
      return false;
    }

    double getUsedThread() {
      double total = 0;
      for (ExecutableRunJob job : runningList) {
        total += job.getRequiredThread();
      }
      return total;
    }

    double getUsedMemory() {
      double total = 0;
      for (ExecutableRunJob job : runningList) {
        total += job.getRequiredMemory();
      }
      return total;
    }

    public Path getRemoteBaseDirectory() {
      return Paths.get(jobManager.getComputer().getWorkBaseDirectory()).resolve(getLocalDirectoryPath());
    }

    public Path getRemoteJobsDirectory() {
      return getRemoteBaseDirectory().resolve(JOBS_PATH);
    }

    public Path getRemoteAliveNotifierDirectory() {
      return getRemoteBaseDirectory().resolve(ALIVE_NOTIFIER_PATH);
    }

    public Path getRemoteLockoutFilePath() {
      return getRemoteBaseDirectory().resolve(LOCKOUT_FILE_PATH);
    }

    public static Path getDirectoryPath(JobManager jobManager, WaffleId id) {
      return jobManager.getDirectoryPath().resolve(id.getReversedBase36Code());
    }
  }
}
