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
import jp.tkms.waffle.data.util.DateTime;
import jp.tkms.waffle.data.util.InstanceCache;
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

public class WrappedSshSubmitter extends JobNumberLimitedSshSubmitter {
  public static final String JOB_MANAGER = "JOB_MANAGER";
  public static final String BIN = "BIN";
  public static final String JOB_MANAGER_JSON_FILE = JOB_MANAGER + Constants.EXT_JSON;
  public static final String RUNNING = "running";
  public static final String ACTIVE = "active";
  public static final String JOB_COUNT = "active";
  public static final String RUN_SH_FILE = "sh ./run.sh";
  public static final String KEY_REMOTE_DIRECTORY = "remote_directory";
  public static final Path JOBS_PATH = Paths.get("JOBS");
  public static final Path ENTITIES_PATH = Paths.get("ENTITIES");
  public static final Path ALIVE_NOTIFIER_PATH = Paths.get("ALIVE_NOTIFIER");
  public static final Path LOCKOUT_FILE_PATH = ALIVE_NOTIFIER_PATH.resolve("LOCKOUT");
  public static final String KEY_ADDITIONAL_PREPARE_COMMAND = "additional_prepare_command";

  private static final InstanceCache<String, Boolean> existsCheckCache = new InstanceCache<>();

  JobManager jobManager;

  public WrappedSshSubmitter(Computer computer) {
    super(computer);
    jobManager = new JobManager(this);
  }

  @Override
  public void submit(AbstractJob job) throws RunNotFoundException {
    if (job.getRun() instanceof VirtualJobExecutor) {
      String execstr =  "";
      try {
        if (job.getState().equals(State.Created)) {
          prepareJob(job);
        }
        execstr =  exec(xsubSubmitCommand(job));
        processXsubSubmit(job, execstr);
      } catch (FailedToControlRemoteException e) {
        WarnLogMessage.issue(job.getComputer(), e.getMessage());
        job.setState(State.Excepted);
      } catch (Exception e) {
        WarnLogMessage.issue(e.getMessage() + ":" + execstr);
        job.setState(State.Excepted);
      }
    } else {
      VirtualJobExecutor executor = jobManager.getNextExecutor(job);
      if (executor != null) {
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
  }

  @Override
  protected boolean isSubmittable(Computer computer, AbstractJob next, ArrayList<AbstractJob> list) {
    JobManager.Submittable submittable = jobManager.getSubmittable(next);
    return (submittable.usableExecutor != null || submittable.isNewExecutorCreatable);
  }

  @Override
  public State update(AbstractJob job) throws RunNotFoundException {
    ComputerTask run = job.getRun();
    if (job instanceof SystemTaskJob) {
      return super.update(job);
    } else {
      String json = "{\"status\":\"" + jobManager.checkStat(job) + "\"}";
      processXstat(job, json);
      return run.getState();
    }
  }

  @Override
  protected void prepareJob(AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    super.prepareJob(job);

    try {
      ComputerTask run = job.getRun();
      if (computer.getParameters().keySet().contains(KEY_ADDITIONAL_PREPARE_COMMAND)) {
        exec("cd '" + getRunDirectory(run) + "';" +computer.getParameter(KEY_ADDITIONAL_PREPARE_COMMAND));
        InfoLogMessage.issue("cd '" + getRunDirectory(run) + "';" +computer.getParameter(KEY_ADDITIONAL_PREPARE_COMMAND));
      }
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }

  @Override
  public void cancel(AbstractJob job) throws RunNotFoundException {
    if (job.getRun() instanceof VirtualJobExecutor) {
      super.cancel(job);
    } else {

    }
  }

  @Override
  public void close() {
    super.close();
    jobManager.close();
  }

  @Override
  protected void cacheClear() {
    super.cacheClear();
    existsCheckCache.clear();
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

    public void close() {
      ArrayList<VirtualJobExecutor> removingList = new ArrayList<>();
      for (VirtualJobExecutor executor : runningExecutorList.values()) {
        if (!executor.getDirectoryPath().toFile().exists()) {
          removingList.add(executor);
        }
      }
      for (VirtualJobExecutor executor : removingList) {
        removeFromArrayOfProperty(RUNNING, executor.getId());
        runningExecutorList.remove(executor.getId());
        removeFromArrayOfProperty(ACTIVE, executor.getId());
        activeExecutorList.remove(executor.getId());
      }
    }

    public VirtualJobExecutor getNextExecutor(AbstractJob next) {
      Submittable submittable = getSubmittable(next);
      VirtualJobExecutor result = submittable.usableExecutor;
      if (result == null && submittable.isNewExecutorCreatable) {
        result = VirtualJobExecutor.create(this, 0, 0);
        registerExecutor(result);
        SystemTaskJob.addRun(result);
      }
      return result;
    }


    static class Submittable {
      boolean isNewExecutorCreatable = false;
      VirtualJobExecutor usableExecutor = null;
      Submittable() {
      }
    }

    protected Submittable getSubmittable(AbstractJob next) {
      ComputerTask nextRun = null;
      try {
        if (next != null) {
          nextRun = next.getRun();
        }
      } catch (RunNotFoundException e) {
      }

      double threadSize = (nextRun == null ? 0 : nextRun.getRequiredThread());
      double memorySize = (nextRun == null ? 0 : nextRun.getRequiredMemory());

      Submittable result = new Submittable();

      ArrayList<VirtualJobExecutor> removingList = new ArrayList<>();
      for (VirtualJobExecutor executor : activeExecutorList.values()) {
        //System.out.println(executor.getId());
        Path directoryPath = executor.getDirectoryPath();
        if (!existsCheckCache.getOrCreate(directoryPath.toString(), k -> directoryPath.toFile().exists())) {
          removeFromArrayOfProperty(RUNNING, executor.getId());
          runningExecutorList.remove(executor.id.getReversedBase36Code());
          removingList.add(executor);
          continue;
        }
        double usedThread = 0.0;
        double usedMemory = 0.0;
        for (AbstractJob abstractJob : executor.getRunningList()) {
          usedThread += abstractJob.getRequiredThread();
          usedMemory += abstractJob.getRequiredMemory();
        }
        if (threadSize <= (getComputer().getMaximumNumberOfThreads() - usedThread)
          && memorySize <= (getComputer().getAllocableMemorySize() - usedMemory)) {
          if (executor.isAcceptable(this)) {
            result.usableExecutor = executor;
            break;
          } else {
            removingList.add(executor);
          }
        }
      }
      if (runningExecutorList.size() < getComputer().getMaximumNumberOfJobs()) {
        result.isNewExecutorCreatable = true;
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
      VirtualJobExecutor executor = VirtualJobExecutor.getInstance(this, WaffleId.valueOf(job.getJobId().replaceFirst("\\..*$", "")));
      boolean finished = true;
      if (executor != null) {
        finished = executor.checkStat(submitter, job);
      }
      return (finished ? "finished" : "running");
    }
  }

  public static class VirtualJobExecutor extends SystemTaskRun {
    WaffleId id;
    //ArrayList<AbstractJob> runningList;
    int jobCount;

    public VirtualJobExecutor(Path path) {
      super(path);
      //this.jobManager = jobManager;
      this.id = WaffleId.valueOf(path.getFileName().toString());
      this.jobCount = getIntFromProperty(JOB_COUNT, -1);
      if (getArrayFromProperty(RUNNING) == null) {
        putNewArrayToProperty(RUNNING);
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
        putToArrayOfProperty(RUNNING, job.getJobId());
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
        VirtualJobExecutor run = null;
        try {
          run = (VirtualJobExecutor) SystemTaskRun.getInstance(getLocalDirectoryPath(jobManager, id).toString());
        } catch (RunNotFoundException e) {
          ErrorLogMessage.issue(e);
        }
        return run;
      }
      return null;
    }

    public static VirtualJobExecutor create(JobManager jobManager, int threadSize, int memorySize) {
      WaffleId id = WaffleId.newId();
      VirtualJobExecutor executor = new VirtualJobExecutor(getDirectoryPath(jobManager, id));
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
      Path workBasePath = Paths.get(jobManager.getComputer().getWorkBaseDirectory());
      try {
        workBasePath = jobManager.submitter.parseHomePath(jobManager.getComputer().getWorkBaseDirectory());
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
      executor.setToProperty(KEY_REMOTE_DIRECTORY, workBasePath.resolve(executor.getLocalDirectoryPath()).toString());
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
      deleteDirectory();

      //TODO: if is run not finished
    }

    @Override
    public void specializedPostProcess(AbstractSubmitter submitter, AbstractJob job) throws OccurredExceptionsException, RunNotFoundException {
      try {
        submitter.exec("rm -rf '" + getRemoteBaseDirectory().toString() + "'");
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
    }

    public boolean isAcceptable(JobManager jobManager) {
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

    ArrayList<AbstractJob> getRunningList() {
      ArrayList<AbstractJob> runningList = new ArrayList<>();
      JSONArray runningArray = getArrayFromProperty(RUNNING);
      for (int i = 0; i < runningArray.length(); i++) {
        for (ExecutableRunJob job : Main.jobStore.getList()) {
          if (job.getJobId().equals(runningArray.getString(i))) {
            runningList.add(job);
            break;
          }
        }
      }
      return runningList;
    }

    public Path getRemoteBaseDirectory() {
      return Paths.get(getStringFromProperty(KEY_REMOTE_DIRECTORY)).resolve(Executable.BASE);
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

    public static Path getLocalDirectoryPath(JobManager jobManager, WaffleId id) {
      return Constants.WORK_DIR.relativize(getDirectoryPath(jobManager, id)).normalize();
    }

    public boolean checkStat(AbstractSubmitter submitter, AbstractJob job) throws RunNotFoundException {
      boolean finished = true;
      try {
        finished = !submitter.exists(getRemoteJobsDirectory().resolve(job.getId().toString()));
        if (finished) {
          removeFromArrayOfProperty(RUNNING, job.getJobId());
        }
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
      return finished;
    }
  }
}
