package jp.tkms.waffle.submitter;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.PollingThread;
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
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.message.request.SubmitJobMessage;
import jp.tkms.waffle.sub.servant.pod.PodTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

public class PodWrappedSubmitter extends AbstractSubmitterWrapper {
  public static final String KEY_TARGET_COMPUTER = "target_computer";
  public static final String JOB_MANAGER = "JOB_MANAGER";
  public static final String BIN = "BIN";
  public static final String JOB_MANAGER_JSON_FILE = JOB_MANAGER + Constants.EXT_JSON;
  public static final String RUNNING = "running";
  public static final String ACTIVE = "active";
  public static final String JOB_COUNT = "job_count";
  public static final String RUN_SH_FILE = "sh ./run.sh";
  public static final String KEY_REMOTE_DIRECTORY = "remote_directory";
  public static final Path JOBS_PATH = Paths.get("JOBS");
  public static final Path ENTITIES_PATH = Paths.get("ENTITIES");
  public static final Path ALIVE_NOTIFIER_PATH = Paths.get("ALIVE_NOTIFIER");
  public static final Path LOCKOUT_FILE_PATH = ALIVE_NOTIFIER_PATH.resolve("LOCKOUT");
  public static final String KEY_ADDITIONAL_PREPARE_COMMAND = "additional_preparation_command";

  //private static final InstanceCache<String, Boolean> existsCheckCache = new InstanceCache<>();

  JobManager jobManager;

  public PodWrappedSubmitter(Computer computer) {
    super(computer);
    jobManager = new JobManager(this);
  }

  @Override
  public void submit(Envelope envelope, AbstractJob job) throws RunNotFoundException {
    if (job.getRun() instanceof VirtualJobExecutor) {
      String execstr =  "";
      try {
        if (job.getState().equals(State.Created)) {
          prepareJob(envelope, job);
        }
        //execstr =  exec(xsubSubmitCommand(job));
        //processXsubSubmit(job, execstr);
        envelope.add(new SubmitJobMessage(job.getTypeCode(), job.getHexCode(), getRunDirectory(job.getRun()), job.getRun().getRemoteBinPath(), BATCH_FILE, computer.getXsubParameters().toString()));
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
          forcePrepare(envelope, job);
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
    if (this.isConnected()) {
      JobManager.Submittable submittable = jobManager.getSubmittable(next);
      return (submittable.usableExecutor != null || submittable.isNewExecutorCreatable);
    } else {
      return jobManager.isReceptable(next, list);
    }
  }

  @Override
  public void update(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException {
    //ComputerTask run = job.getRun();
    if (job instanceof SystemTaskJob) {
      super.update(envelope, job);
    } else {
      //String json = "{\"status\":\"" + jobManager.checkStat(job) + "\"}";
      //processXstat(job, json);
      //return run.getState();

      try {
        if (jobManager.checkStat(job)) {
          job.setState(State.Finalizing);
          finishedProcessorManager.startup();
          preparingProcessorManager.startup();
        } else {
          if (job.getState().equals(State.Submitted)) {
            job.setState(State.Running);
          }
        }
      } catch (RunNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void prepareJob(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    super.prepareJob(envelope, job);

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
  public void cancel(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException {
    if (job.getRun() instanceof VirtualJobExecutor) {
      super.cancel(envelope, job);
    } else {
      job.setState(State.Canceled);
      if (! job.getJobId().equals("-1")) {
        jobManager.removeJob(job);
      }
    }
  }

  @Override
  public void close() {
    super.close();
    jobManager.close();
  }

  /*
  @Override
  protected void cacheClear() {
    super.cacheClear();
    existsCheckCache.clear();
  }
   */

  public static class JobManager implements DataDirectory, PropertyFile {
    PodWrappedSubmitter submitter;
    TreeMap<String, VirtualJobExecutor> runningExecutorList;
    TreeMap<String, VirtualJobExecutor> activeExecutorList;
    VirtualJobExecutor submittableCache = null;
    SystemTaskJob nextExecutorJob = null;

    public JobManager(PodWrappedSubmitter submitter) {
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
        VirtualJobExecutor executor = VirtualJobExecutor.getInstance(this, waffleId);
        if (executor != null) {
          runningExecutorList.put(waffleId.getReversedBase36Code(), executor);
        } else {
          removeFromArrayOfProperty(RUNNING, waffleId.getReversedBase36Code());
        }
      }
      JSONArray activeArray = getArrayFromProperty(ACTIVE);
      for (int i = 0; i < activeArray.length(); i++) {
        WaffleId waffleId = WaffleId.valueOf(activeArray.getString(i));
        VirtualJobExecutor executor = VirtualJobExecutor.getInstance(this, waffleId);
        if (executor != null) {
          activeExecutorList.put(waffleId.getReversedBase36Code(), executor);
        } else {
          removeFromArrayOfProperty(ACTIVE, waffleId.getReversedBase36Code());
        }
      }
    }

    public void close() {
      ArrayList<VirtualJobExecutor> removingList = new ArrayList<>();
      for (VirtualJobExecutor executor : runningExecutorList.values()) {
        try {
          if (!executor.getDirectoryPath().toFile().exists()) {
            removingList.add(executor);
          }
        } catch (Exception e) {
          ErrorLogMessage.issue(e);
        }
      }
      for (VirtualJobExecutor executor : removingList) {
        removeFromArrayOfProperty(RUNNING, executor.getId());
        runningExecutorList.remove(executor.getId());
        removeFromArrayOfProperty(ACTIVE, executor.getId());
        activeExecutorList.remove(executor.getId());
      }

      if (nextExecutorJob != null) {
        try {
          nextExecutorJob.cancel();
        } catch (RunNotFoundException e) {
          //NOP
        }
      }
    }

    public VirtualJobExecutor getNextExecutor(AbstractJob next) throws RunNotFoundException {
      Submittable submittable = getSubmittable(next);
      VirtualJobExecutor result = submittable.usableExecutor;
      if (result == null && submittable.isNewExecutorCreatable) {
        nextExecutorJob.replaceComputer(submittable.targetComputer);
        result = (VirtualJobExecutor) nextExecutorJob.getRun();
        registerExecutor(result);
        nextExecutorJob = null;
      }
      return result;
    }

    static class Submittable {
      Computer targetComputer = null;
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

      for (VirtualJobExecutor executor : new ArrayList<>(runningExecutorList.values())) {
        Path directoryPath = executor.getDirectoryPath();
        if (!directoryPath.toFile().exists() || !executor.isRunning()) {
          deactivateExecutor(executor);
          removeFromArrayOfProperty(RUNNING, executor.getId());
          runningExecutorList.remove(executor.id.getReversedBase36Code());
          if (directoryPath.toFile().exists()) {
            executor.deleteDirectory();
          }
        }
      }

      ArrayList<VirtualJobExecutor> shuffledList = new ArrayList<>(activeExecutorList.values());
      Collections.shuffle(shuffledList);
      ArrayList<VirtualJobExecutor> cacheAppliedList = new ArrayList<>();
      if (submittableCache != null && shuffledList.contains(submittableCache)) {
        cacheAppliedList.add(submittableCache);
        shuffledList.remove(submittableCache);
        cacheAppliedList.addAll(shuffledList);
      } else {
        cacheAppliedList.addAll(shuffledList);
      }
      for (VirtualJobExecutor executor : cacheAppliedList) {
        Path directoryPath = executor.getDirectoryPath();
        if (!directoryPath.toFile().exists()) {
          //if (!existsCheckCache.getOrCreate(directoryPath.toString(), k -> directoryPath.toFile().exists())) {
          deactivateExecutor(executor);
          removeFromArrayOfProperty(RUNNING, executor.getId());
          runningExecutorList.remove(executor.id.getReversedBase36Code());
          continue;
        }
        //System.out.println(executor.getId());
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
            submittableCache = executor;
            break;
          } else {
            deactivateExecutor(executor);
          }
        }
      }

      result.targetComputer = Computer.getInstance(getComputer().getParameters().getString(KEY_TARGET_COMPUTER));
      if (result.targetComputer != null) {
        if (nextExecutorJob == null) {
          nextExecutorJob = SystemTaskJob.addRun(VirtualJobExecutor.create(this, getComputer().getMaximumNumberOfThreads(), getComputer().getAllocableMemorySize()));
        }
        AbstractSubmitter targetSubmitter = AbstractSubmitter.getInstance(PollingThread.Mode.Normal, result.targetComputer);
        result.isNewExecutorCreatable = targetSubmitter.isSubmittable(result.targetComputer, nextExecutorJob);
      }
      /*
      if (runningExecutorList.size() < getComputer().getMaximumNumberOfJobs()) {
        result.isNewExecutorCreatable = true;
      }
       */

      return result;
    }

    public boolean isReceptable(AbstractJob next, ArrayList<AbstractJob> list) {
      for (VirtualJobExecutor executor : new ArrayList<>(runningExecutorList.values())) {
        Path directoryPath = executor.getDirectoryPath();
        if (!directoryPath.toFile().exists() || !executor.isRunning()) {
          deactivateExecutor(executor);
          removeFromArrayOfProperty(RUNNING, executor.getId());
          runningExecutorList.remove(executor.id.getReversedBase36Code());
          if (directoryPath.toFile().exists()) {
            executor.deleteDirectory();
          }
        }
      }

      ArrayList<AbstractJob> queuedList = new ArrayList<>();
      queuedList.add(next);
      for (AbstractJob job : list) {
        try {
          switch (job.getState()) {
            case Created:
            case Prepared:
              queuedList.add(job);
          }
        } catch (RunNotFoundException e) {
          ErrorLogMessage.issue(e);
        }
      }

      ArrayList<VirtualJobExecutor> shuffledList = new ArrayList<>(activeExecutorList.values());
      Collections.shuffle(shuffledList);
      ArrayList<VirtualJobExecutor> cacheAppliedList = new ArrayList<>();
      if (submittableCache != null && shuffledList.contains(submittableCache)) {
        cacheAppliedList.add(submittableCache);
        shuffledList.remove(submittableCache);
        cacheAppliedList.addAll(shuffledList);
      } else {
        cacheAppliedList.addAll(shuffledList);
      }
      for (VirtualJobExecutor executor : cacheAppliedList) {
        Path directoryPath = executor.getDirectoryPath();
        if (!directoryPath.toFile().exists()) {
          //if (!existsCheckCache.getOrCreate(directoryPath.toString(), k -> directoryPath.toFile().exists())) {
          deactivateExecutor(executor);
          removeFromArrayOfProperty(RUNNING, executor.getId());
          runningExecutorList.remove(executor.id.getReversedBase36Code());
          continue;
        }

        double usedThread = 0.0;
        double usedMemory = 0.0;
        for (AbstractJob abstractJob : executor.getRunningList()) {
          usedThread += abstractJob.getRequiredThread();
          usedMemory += abstractJob.getRequiredMemory();
        }
        //System.out.println(executor.getId());
        for (int i = queuedList.size() -1; i >= 0; i -= 1) {
          ComputerTask task = null;
          try {
            if (next != null) {
              task = next.getRun();
            }
          } catch (RunNotFoundException e) {
          }

          double threadSize = (task == null ? 0 : task.getRequiredThread());
          double memorySize = (task == null ? 0 : task.getRequiredMemory());

          if (threadSize <= (getComputer().getMaximumNumberOfThreads() - usedThread)
            && memorySize <= (getComputer().getAllocableMemorySize() - usedMemory)) {
            if (queuedList.size() <= 1) {
              return true;
            } else {
              queuedList.remove(i);
              usedThread += threadSize;
              usedMemory += memorySize;
            }
          }
        }
      }

      for (int count = 0; count < getComputer().getMaximumNumberOfJobs() - runningExecutorList.size(); count += 1) {
        double usedThread = 0.0;
        double usedMemory = 0.0;

        for (int i = queuedList.size() -1; i >= 0; i -= 1) {
          ComputerTask task = null;
          try {
            if (next != null) {
              task = next.getRun();
            }
          } catch (RunNotFoundException e) {
          }

          double threadSize = (task == null ? 0 : task.getRequiredThread());
          double memorySize = (task == null ? 0 : task.getRequiredMemory());

          if (threadSize <= (getComputer().getMaximumNumberOfThreads() - usedThread)
            && memorySize <= (getComputer().getAllocableMemorySize() - usedMemory)) {
            if (queuedList.size() <= 1) {
              return true;
            } else {
              queuedList.remove(i);
              usedThread += threadSize;
              usedMemory += memorySize;
            }
          } else {
            break;
          }
        }
      }

      return false;
    }

    void removeJob(AbstractJob job) throws RunNotFoundException {
      for (VirtualJobExecutor executor : new ArrayList<>(runningExecutorList.values())) {
        if (executor.getRunningList().contains(job)) {
          executor.cancel(submitter, job);
          break;
        }
      }
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

    public boolean checkStat(AbstractJob job) throws RunNotFoundException {
      String executorId = job.getJobId().replaceFirst("\\..*$", "");
      VirtualJobExecutor executor = runningExecutorList.get(executorId);
      if (executor == null) {
        executor = VirtualJobExecutor.getInstance(this, WaffleId.valueOf(executorId));
      }
      boolean finished = true;
      if (executor != null) {
        finished = executor.checkStat(submitter, job);
      }
      return finished;
    }
  }

  public static class VirtualJobExecutor extends SystemTaskRun {
    WaffleId id;
    //ArrayList<AbstractJob> runningList;
    int jobCount;
    HashMap<String, AbstractJob> jobCache = new HashMap<>();

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
        submitter.chmod(777, getRemoteEntitiesDirectory());
        submitter.chmod(777, getRemoteJobsDirectory());
        submitter.putText(job, getRemoteEntitiesDirectory().resolve(job.getId().toString()), runDirectoryPath.toString());
        submitter.chmod(666, getRemoteEntitiesDirectory().resolve(job.getId().toString()));
        submitter.putText(job, getRemoteJobsDirectory().resolve(job.getId().toString()), "");
        submitter.chmod(666, getRemoteJobsDirectory().resolve(job.getId().toString()));
        job.setJobId(id.getReversedBase36Code() + '.' + getNextJobCount());
        job.setState(State.Submitted);
        putToArrayOfProperty(RUNNING, job.getJobId());
        jobCache.put(job.getJobId(), job);
        InfoLogMessage.issue(job.getRun(), "was submitted");
      } catch (FailedToTransferFileException | FailedToControlRemoteException e) {
        job.setState(State.Excepted);
      }
    }

    public void cancel(AbstractSubmitter submitter, AbstractJob job) throws RunNotFoundException {
      try {
        submitter.deleteFile(getRemoteJobsDirectory().resolve(job.getId().toString()));
      } catch (FailedToControlRemoteException e) {
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

    public static VirtualJobExecutor create(JobManager jobManager, double threadSize, double memorySize) {
      WaffleId id = WaffleId.newId();
      VirtualJobExecutor executor = new VirtualJobExecutor(getDirectoryPath(jobManager, id));
      executor.setCommand(PodTask.PODTASK);
      executor.addArgument(true);
      executor.addArgument(120);
      executor.addArgument(259100);
      executor.addArgument(3600);
      executor.setBinPath(null);
      executor.setRequiredThread(threadSize);
      executor.setRequiredMemory(memorySize);
      executor.setComputer(jobManager.getComputer());
      executor.setState(State.Created);
      executor.setToProperty(KEY_EXIT_STATUS, -1);
      executor.setToProperty(KEY_CREATED_AT, DateTime.getCurrentEpoch());
      executor.setToProperty(KEY_SUBMITTED_AT, DateTime.getEmptyEpoch());
      executor.setToProperty(KEY_FINISHED_AT, DateTime.getEmptyEpoch());
      /*
      Path workBasePath = Paths.get(jobManager.getComputer().getWorkBaseDirectory());
      try {
        workBasePath = jobManager.submitter.parseHomePath(jobManager.getComputer().getWorkBaseDirectory());
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
      executor.setToProperty(KEY_REMOTE_DIRECTORY, workBasePath.resolve(executor.getLocalDirectoryPath()).toString());
       */
      executor.setToProperty(KEY_REMOTE_DIRECTORY, executor.getLocalDirectoryPath().toString());
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
      /*
      try {
        submitter.exec("rm -rf '" + getRemoteBaseDirectory().toString() + "'");
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
       */
    }

    public boolean isAcceptable(JobManager jobManager) {
      switch (getState()) {
        case Created:
        case Prepared:
        case Submitted:
        case Running:
          try {
            if (!jobManager.submitter.isConnected() || !jobManager.fileExists(getRemoteLockoutFilePath())) {
              return true;
            }
          } catch (FailedToControlRemoteException e) {
            ErrorLogMessage.issue(e);
          }
      }
      return false;
    }

    public ArrayList<AbstractJob> getRunningList() {
      ArrayList<AbstractJob> runningList = new ArrayList<>();
      JSONArray runningArray = getArrayFromProperty(RUNNING);
      for (int i = 0; i < runningArray.length(); i++) {
        if (jobCache.containsKey(runningArray.getString(i))) {
          runningList.add(jobCache.get(runningArray.getString(i)));
        }
      }
      for (ExecutableRunJob job : Main.jobStore.getList()) {
        if (runningList.size() >= runningArray.length()) {
          break;
        }
        if (runningArray.toList().contains(job.getJobId())) {
          runningList.add(job);
          jobCache.put(job.getJobId(), job);
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
          jobCache.remove(job.getJobId());
        }
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
      }
      return finished;
    }
  }


  @Override
  public AbstractSubmitter connect(boolean retry) {
    return this;
  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public Path parseHomePath(String pathString) throws FailedToControlRemoteException {
    return null;
  }

  @Override
  public void createDirectories(Path path) throws FailedToControlRemoteException {

  }

  @Override
  boolean exists(Path path) throws FailedToControlRemoteException {
    return false;
  }

  @Override
  boolean deleteFile(Path path) throws FailedToControlRemoteException {
    return false;
  }

  @Override
  public String exec(String command) throws FailedToControlRemoteException {
    return null;
  }

  @Override
  public void putText(AbstractJob job, Path path, String text) throws FailedToTransferFileException, RunNotFoundException {

  }

  @Override
  public String getFileContents(ComputerTask run, Path path) throws FailedToTransferFileException {
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {

  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {

  }

  @Override
  public JSONObject getDefaultParameters(Computer computer) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_TARGET_COMPUTER, "LOCAL");
    return jsonObject;
  }
}
