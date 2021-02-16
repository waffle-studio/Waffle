package jp.tkms.waffle.submitter;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.PropertyFile;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.SystemTaskRun;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.SystemTaskJob;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;

public class WrappedSshSubmitter extends SshSubmitter {
  public static final String JOB_MANAGER = "JOB_MANAGER";
  public static final String BIN = "BIN";
  public static final String JOB_MANAGER_JSON_FILE = JOB_MANAGER + Constants.EXT_JSON;
  public static final String RUNNING = "running";
  public static final String ACTIVE = "active";
  public static final String JOB_COUNT = "active";
  public static final String RUN_SH_FILE = "./run.sh";
  public static final Path JOBS_PATH = Paths.get("JOBS");
  public static final Path ENTITIES_PATH = Paths.get("ENTITIES");
  public static final Path ALIVE_NOTIFIER_PATH = Paths.get("ALIVE_NOTIFIER");
  public static final Path LOCKOUT_FILE_PATH = ALIVE_NOTIFIER_PATH.resolve("LOCKOUT");

  JobManager jobManager;

  public WrappedSshSubmitter(Computer computer) {
    super(computer);
    jobManager = new JobManager(this);
  }

  @Override
  public void submit(AbstractJob job) throws RunNotFoundException {
    if (job.getRun() instanceof VirtualJobExecutor) {
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
    } else {
      VirtualJobExecutor executor = jobManager.getNextExecutor(1,1);
      try {
        if (job.getState().equals(State.Created)) {
          prepareJob(job);
        }
        executor.submit(this, job);
      } catch (FailedToControlRemoteException e) {
        WarnLogMessage.issue(job.getComputer(), e.getMessage());
        job.setState(State.Excepted);
      } catch (Exception e) {
        WarnLogMessage.issue(e);
        job.setState(State.Excepted);
      }
    }
  }

  @Override
  public State update(AbstractJob job) throws RunNotFoundException {
    ComputerTask run = job.getRun();
    if (run instanceof VirtualJobExecutor) {
      return super.update(job);
    } else {
      String json = "{\"status\":\"" + jobManager.checkStat(job) + "\"}";
      processXstat(job, json);
      return run.getState();
    }
  }

  @Override
  public void cancel(AbstractJob job) throws RunNotFoundException {
    if (job.getRun() instanceof VirtualJobExecutor) {
      super.cancel(job);
    } else {

    }
  }

  public static class JobManager implements DataDirectory, PropertyFile {
    WrappedSshSubmitter submitter;
    TreeMap<String, VirtualJobExecutor> runningExecutorList;
    TreeMap<String, VirtualJobExecutor> activeExecutorList;

    public JobManager(WrappedSshSubmitter submitter) {
      this.submitter = submitter;

      try {
        Files.createDirectories(getDirectoryPath());
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
      if (getArrayFromProperty(RUNNING) == null) {
        putNewArrayToProperty(RUNNING);
      }
      if (getArrayFromProperty(ACTIVE) == null) {
        putNewArrayToProperty(ACTIVE);
      }
      runningExecutorList = new TreeMap<>();
      activeExecutorList = new TreeMap<>();
      JSONArray runningArray = getArrayFromProperty(RUNNING);
      for (int i = 0; i < runningArray.length(); i++) {
        WaffleId waffleId = WaffleId.valueOf(runningArray.getString(i));
        runningExecutorList.put(waffleId.getReversedBase36Code(), VirtualJobExecutor.getInstance(this, waffleId));
      }
      JSONArray activeArray = getArrayFromProperty(ACTIVE);
      for (int i = 0; i < activeArray.length(); i++) {
        WaffleId waffleId = WaffleId.valueOf(activeArray.getString(i));
        activeExecutorList.put(waffleId.getReversedBase36Code(), VirtualJobExecutor.getInstance(this, waffleId));
      }
    }

    public VirtualJobExecutor getNextExecutor(double threadSize, double memorySize) {
      VirtualJobExecutor result = null;
      ArrayList<VirtualJobExecutor> removingList = new ArrayList<>();
      for (VirtualJobExecutor executor : activeExecutorList.values()) {
        if (threadSize <= (submitter.getMaximumNumberOfThreads(submitter.computer) - executor.getUsedThread())
          && memorySize <= (submitter.getAllocableMemorySize(submitter.computer) - executor.getUsedMemory())) {
          if (executor.isAcceptable()) {
            result = executor;
            break;
          } else {
            removingList.add(executor);
          }
        }
      }
      if (result == null) {
        result = VirtualJobExecutor.create(this, 0, 0);
        registerExecutor(result);
        SystemTaskJob.addRun(result);
      }
      for (VirtualJobExecutor executor : removingList) {
        deactivateExecutor(executor);
      }
      return result;
    }

    void registerExecutor(VirtualJobExecutor executor) {
      putToArrayOfProperty(RUNNING, executor.getId());
      runningExecutorList.put(executor.id.getReversedBase36Code(), executor);
      putToArrayOfProperty(ACTIVE, executor.getId());
      activeExecutorList.put(executor.id.getReversedBase36Code(), executor);
    }

    void removeExecutor(VirtualJobExecutor executor) {
      removeFromArrayOfProperty(RUNNING, executor.getId());
      runningExecutorList.remove(executor.id.getReversedBase36Code());
      removeFromArrayOfProperty(ACTIVE, executor.getId());
      activeExecutorList.remove(executor.id.getReversedBase36Code());
    }

    void deactivateExecutor(VirtualJobExecutor executor) {
      removeFromArrayOfProperty(ACTIVE, executor.getId());
      activeExecutorList.remove(executor.id.getReversedBase36Code());
    }

    boolean fileExists(Path path) throws FailedToControlRemoteException {
      return submitter.exists(path);
    }

    public Computer getComputer() {
      return submitter.computer;
    }

    @Override
    public Path getDirectoryPath() {
      return submitter.computer.getDirectoryPath().resolve(JOB_MANAGER);
    }

    @Override
    public Path getPropertyStorePath() {
      return getDirectoryPath().resolve(JOB_MANAGER_JSON_FILE);
    }

    JSONObject jsonObject = null;
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

    public String checkStat(AbstractJob job) throws RunNotFoundException {
      VirtualJobExecutor executor = runningExecutorList.get(job.getJobId().replaceFirst("\\..*$", ""));
      boolean finished = true;
      if (executor != null) {
        finished = executor.checkStat(submitter, job);
      }
      return (finished ? "finished" : "running");
    }
  }

  static class VirtualJobExecutor extends SystemTaskRun {
    JobManager jobManager;
    WaffleId id;
    ArrayList<ExecutableRunJob> runningList;
    int jobCount;

    VirtualJobExecutor(JobManager jobManager, WaffleId id) {
      super(getDirectoryPath(jobManager, id));
      this.jobManager = jobManager;
      this.id = id;
      this.jobCount = getIntFromProperty(JOB_COUNT, -1);
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

    public int getNextJobCount() {
      jobCount += 1;
      setToProperty(JOB_COUNT, jobCount);
      return jobCount;
    }

    public void submit(AbstractSubmitter submitter, AbstractJob job) throws RunNotFoundException {
      try {
        Path runDirectoryPath = submitter.getRunDirectory(job.getRun());
        submitter.createDirectories(getRemoteEntitiesDirectory());
        submitter.createDirectories(getRemoteJobsDirectory());
        submitter.putText(job, getRemoteEntitiesDirectory().resolve(job.getId().toString()), runDirectoryPath.toString());
        submitter.putText(job, getRemoteJobsDirectory().resolve(job.getId().toString()), "");
        job.setJobId(id.getReversedBase36Code() + '.' + getNextJobCount());
        job.setState(State.Submitted);
        InfoLogMessage.issue(job.getRun(), "was submitted");
      } catch (FailedToTransferFileException | FailedToControlRemoteException e) {
        job.setState(State.Excepted);
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

    @Override
    public void setState(State state) {
      if (getDirectoryPath().toFile().exists()) {
        super.setState(state);
      }
    }

    @Override
    public void finish() {
      super.finish();
      jobManager.removeExecutor(this);
      deleteDirectory();
    }

    @Override
    public void specializedPostProcess(AbstractSubmitter submitter, AbstractJob job) throws OccurredExceptionsException, RunNotFoundException {
      try {
        submitter.exec("rm -rf '" + getRemoteBaseDirectory().toString() + "'");
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
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
      return Paths.get(jobManager.getComputer().getWorkBaseDirectory()).resolve(getLocalDirectoryPath()).resolve(Executable.BASE);
    }

    public Path getRemoteJobsDirectory() {
      return getRemoteBaseDirectory().resolve(JOBS_PATH);
    }

    public Path getRemoteEntitiesDirectory() {
      return getRemoteBaseDirectory().resolve(ENTITIES_PATH);
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

    public boolean checkStat(AbstractSubmitter submitter, AbstractJob job) throws RunNotFoundException {
      boolean finished = true;
      try {
        finished = !submitter.exists(getRemoteJobsDirectory().resolve(job.getId().toString()));
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
      return finished;
    }
  }
}
