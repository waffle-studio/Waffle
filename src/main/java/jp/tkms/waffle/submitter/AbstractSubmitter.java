package jp.tkms.waffle.submitter;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.ServantJarFile;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.SystemTaskJob;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.*;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.TaskJson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

abstract public class AbstractSubmitter {
  protected static final String DOT_ENVELOPE = ".ENVELOPE";
  protected static final String TASK_JSON = "task.json";
  protected static final String RUN_DIR = "run";
  protected static final String BATCH_FILE = "batch.sh";
  protected static final String ARGUMENTS_FILE = "arguments.txt";

  boolean isRunning = false;
  Computer computer;
  protected static ExecutorService threadPool = Executors.newFixedThreadPool(4);
  private int pollingInterval = 5;
  private int preparingNumber = 1;
  private PollingThread.Mode mode;

  ProcessorManager<CreatedProcessor> createdProcessorManager = new ProcessorManager<>(() -> new CreatedProcessor());
  ProcessorManager<PreparedProcessor> preparedProcessorManager = new ProcessorManager<>(() -> new PreparedProcessor());
  ProcessorManager<FinishedProcessor> finishedProcessorManager = new ProcessorManager<>(() -> new FinishedProcessor());

  public int getPollingInterval() {
    return pollingInterval;
  }

  public void skipPolling() {
    pollingInterval = 0;
  }

  abstract public AbstractSubmitter connect(boolean retry);
  abstract public boolean isConnected();

  abstract public JSONObject getDefaultParameters(Computer computer);

  abstract public Path parseHomePath(String pathString) throws FailedToControlRemoteException;

  abstract public void createDirectories(Path path) throws FailedToControlRemoteException;
  abstract boolean exists(Path path) throws FailedToControlRemoteException;
  abstract public String exec(String command) throws FailedToControlRemoteException;
  abstract public void putText(AbstractJob job, Path path, String text) throws FailedToTransferFileException, RunNotFoundException;
  abstract public String getFileContents(ComputerTask run, Path path) throws FailedToTransferFileException;
  abstract public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException;
  abstract public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException;

  public AbstractSubmitter(Computer computer) {
    this.computer = computer;
  }

  public AbstractSubmitter connect() {
    return connect(true);
  }

  public void putText(AbstractJob job, String pathString, String text) throws FailedToTransferFileException, RunNotFoundException {
    putText(job, Paths.get(pathString), text);
  }

  public static AbstractSubmitter getInstance(PollingThread.Mode mode, Computer computer) {
    AbstractSubmitter submitter = null;
    try {
      Class<?> clazz = Class.forName(computer.getSubmitterType());
      Constructor<?> constructor = clazz.getConstructor(Computer.class);
      submitter = (AbstractSubmitter) constructor.newInstance(new Object[]{computer});
    }catch(Exception e) {
      ErrorLogMessage.issue(e);
    }

    if (submitter == null) {
      submitter = new JobNumberLimitedSshSubmitter(computer);
    }

    submitter.mode = mode;

    return submitter;
  }

  public void chmod(int mod, Path path) throws FailedToControlRemoteException {
    exec("chmod " + mod +" '" + path.toString() + "'");
  }

  public Envelope sendAndReceiveEnvelope(Envelope envelope) throws Exception {
    Path tmpFile = Files.createTempDirectory(Constants.APP_NAME).resolve(UUID.randomUUID().toString());
    Files.createDirectories(tmpFile.getParent());
    envelope.save(tmpFile);
    Path remoteWorkBasePath = parseHomePath(computer.getWorkBaseDirectory());
    Path remoteEnvelopePath = remoteWorkBasePath.resolve(DOT_ENVELOPE).resolve(tmpFile.getFileName());
    createDirectories(remoteEnvelopePath.getParent());
    transferFilesToRemote(tmpFile, remoteEnvelopePath);
    Files.delete(tmpFile);
    exec("java -jar '" + getWaffleServantPath(this, computer)
      + "' '" + remoteWorkBasePath + "' main '" + remoteEnvelopePath + "'");
    Path remoteResponsePath = Envelope.getResponsePath(remoteEnvelopePath);
    transferFilesFromRemote(remoteResponsePath, tmpFile);
    exec("rm '" + remoteResponsePath + "'");
    Envelope response = Envelope.loadAndExtract(Constants.WORK_DIR, tmpFile);
    Files.delete(tmpFile);
    return response;
  }

  public void forcePrepare(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    if (job.getState().equals(State.Created)) {
      preparingNumber += 1;
      prepareJob(envelope, job);
    }
  }

  public void submit(Envelope envelope, AbstractJob job) throws RunNotFoundException {
    try {
      forcePrepare(envelope, job);
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

  public State update(AbstractJob job) throws RunNotFoundException {
    ComputerTask run = job.getRun();
    try {
      processXstat(job, exec(xstatCommand(job)));
    } catch (FailedToControlRemoteException e) {
      ErrorLogMessage.issue(e);
    }
    return run.getState();
  }

  public void cancel(AbstractJob job) throws RunNotFoundException {
    job.setState(State.Canceled);
    if (! job.getJobId().equals("-1")) {
      try {
        processXdel(job, exec(xdelCommand(job)));
      } catch (FailedToControlRemoteException e) {
        ErrorLogMessage.issue(e);
        job.setState(State.Excepted);
      }
    }
  }

  protected void prepareJob(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    synchronized (job) {
      if (job.getState().equals(State.Created)) {
        ComputerTask run = job.getRun();
        run.setRemoteWorkingDirectoryLog(getRunDirectory(run).toString());

        //run.getSimulator().updateVersionId();

        //putText(job, BATCH_FILE, makeBatchFileText(job));
        //putText(job, EXIT_STATUS_FILE, "-2");
        String projectName = (run instanceof ExecutableRun ? ((ExecutableRun)run).getProject().getName() : ".SYSTEM_TASK");
        JSONArray localSharedList = (run instanceof ExecutableRun ? ((ExecutableRun) run).getLocalSharedList() : new JSONArray());
        JsonObject localShared = new JsonObject();
        for (int i = 0; i < localSharedList.length(); i++) {
          JSONArray a = localSharedList.getJSONArray(i);
          localShared.add(a.getString(0), a.getString(1));
        }

        run.specializedPreProcess(this);

        JsonArray arguments = new JsonArray();
        for (Object object : run.getArguments()) {
          arguments.add(object.toString());
        }
        JsonObject environments = new JsonObject();
        for (Map.Entry<String, Object> entry : run.getActualComputer().getEnvironments().toMap().entrySet()) {
          environments.add(entry.getKey(), entry.getValue().toString());
        }
        for (Map.Entry<String, Object> entry : run.getEnvironments().toMap().entrySet()) {
          environments.add(entry.getKey(), entry.getValue().toString());
        }

        TaskJson taskJson = new TaskJson(projectName, run.getRemoteBinPath().toString(), run.getCommand(),
          arguments, environments, localShared);
        putText(job, TASK_JSON, taskJson.toString());

        putText(job, BATCH_FILE, "java -jar '" + getWaffleServantPath(this, computer)
          + "' '" + parseHomePath(computer.getWorkBaseDirectory()) + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'");

        //putText(job, ARGUMENTS_FILE, makeArgumentFileText(job));
        //putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

        if (! exists(getExecutableBaseDirectory(job).toAbsolutePath())) {
          //Path binPath = run.getExecutable().getBaseDirectory().toAbsolutePath();
          envelope.add(run.getBinPath());
          //transferFilesToRemote(run.getBinPath(), getExecutableBaseDirectory(job).toAbsolutePath());
        }

        envelope.add(run.getBasePath());
        //Path work = run.getBasePath();
        //transferFilesToRemote(work, getRunDirectory(run).resolve(work.getFileName()));

        job.setState(State.Prepared);
        InfoLogMessage.issue(job.getRun(), "was prepared");
      }
    }
  }

  public Path getBaseDirectory(ComputerTask run) throws FailedToControlRemoteException {
    return getRunDirectory(run).resolve(Executable.BASE);
  }

  public Path getRunDirectory(ComputerTask run) throws FailedToControlRemoteException {
    Computer computer = run.getActualComputer();
    Path path = parseHomePath(computer.getWorkBaseDirectory()).resolve(run.getLocalDirectoryPath());

    createDirectories(path);

    return path;
  }

  Path getExecutableBaseDirectory(AbstractJob job) throws FailedToControlRemoteException, RunNotFoundException {
    return parseHomePath(job.getComputer().getWorkBaseDirectory()).resolve(job.getRun().getRemoteBinPath());
  }

  String makeBatchFileText(AbstractJob job) throws FailedToControlRemoteException, RunNotFoundException {
    ComputerTask run = job.getRun();
    JSONArray localSharedList = (run instanceof ExecutableRun ? ((ExecutableRun) run).getLocalSharedList() : new JSONArray());
    String projectName = (run instanceof ExecutableRun ? ((ExecutableRun)run).getProject().getName() : ".SYSTEM_TASK");

    String text = "#!/bin/sh\n" +
      "\n" +
      "export WAFFLE_BASE='" + getExecutableBaseDirectory(job) + "'\n" +
      "export WAFFLE_BATCH_WORKING_DIR=`pwd`\n" +
      "touch '" + Constants.STDOUT_FILE +"'\n" +
      "touch '" + Constants.STDERR_FILE +"'\n" +
      "mkdir -p '" + getBaseDirectory(run) +"'\n" +
      "cd '" + getBaseDirectory(run) + "'\n" +
      "export WAFFLE_WORKING_DIR=`pwd`\n" +
      "cd '" + getExecutableBaseDirectory(job) + "'\n" +
      "ls -l >/dev/null 2>&1\n" +
      "chmod a+x '" + run.getCommand() + "' >/dev/null 2>&1\n" +
      "ls -l >/dev/null 2>&1\n" +
      "find . -type d | xargs -n 1 -I{1} sh -c 'mkdir -p \"${WAFFLE_WORKING_DIR}/{1}\";find {1} -maxdepth 1 -type f | xargs -n 1 -I{2} ln -s \"`pwd`/{2}\" \"${WAFFLE_WORKING_DIR}/{1}/\"'\n" +
      "cd \"${WAFFLE_BATCH_WORKING_DIR}\"\n" +
      "export WAFFLE_LOCAL_SHARED=\"" + job.getComputer().getWorkBaseDirectory().replaceFirst("^~", "\\$\\{HOME\\}") + "/local_shared/" + projectName + "\"\n" +
      "mkdir -p \"$WAFFLE_LOCAL_SHARED\"\n" +
      "cd \"${WAFFLE_WORKING_DIR}\"\n";

    for (int i = 0; i < localSharedList.length(); i++) {
      JSONArray a = localSharedList.getJSONArray(i);
      text += makeLocalSharingPreCommandText(a.getString(0), a.getString(1));
    }

    text += makeEnvironmentCommandText(job);

    text += "\n" + run.getCommand() + " >\"${WAFFLE_BATCH_WORKING_DIR}/" + Constants.STDOUT_FILE + "\" 2>\"${WAFFLE_BATCH_WORKING_DIR}/" + Constants.STDERR_FILE + "\" `cat \"${WAFFLE_BATCH_WORKING_DIR}/" + ARGUMENTS_FILE + "\"`\n" +
      "EXIT_STATUS=$?\n";

    for (int i = 0; i < localSharedList.length(); i++) {
      JSONArray a = localSharedList.getJSONArray(i);
      text += makeLocalSharingPostCommandText(a.getString(0), a.getString(1));
    }

    //text += "\n" + "cd \"${WAFFLE_BATCH_WORKING_DIR}\"\n" + "echo ${EXIT_STATUS} > " + EXIT_STATUS_FILE + "\n" + "\n";

    return text;
  }

  String makeLocalSharingPreCommandText(String key, String remote) {
    return "mkdir -p `dirname \"" + remote + "\"`;if [ -e \"${WAFFLE_LOCAL_SHARED}/" + key + "\" ]; then ln -fs \"${WAFFLE_LOCAL_SHARED}/" + key + "\" \"" + remote + "\"; else echo \"" + key + "\" >> \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; fi\n";
  }

  String makeLocalSharingPostCommandText(String key, String remote) {
    return "if grep \"^" + key + "$\" \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; then mv \"" + remote + "\" \"${WAFFLE_LOCAL_SHARED}/" + key + "\"; ln -fs \"${WAFFLE_LOCAL_SHARED}/"  + key + "\" \"" + remote + "\" ;fi\n";
  }

  String makeArgumentFileText(AbstractJob job) throws RunNotFoundException {
    String text = "";
    for (Object o : job.getRun().getArguments()) {
      text += o.toString() + "\n";
    }
    return text;
  }

  String makeEnvironmentCommandText(AbstractJob job) throws RunNotFoundException {
    String text = "";
    for (Map.Entry<String, Object> entry : job.getComputer().getEnvironments().toMap().entrySet()) {
      text += "export " + entry.getKey().replace(' ', '_') + "=\"" + entry.getValue().toString().replace("\"", "\\\"") + "\"\n";
    }
    for (Map.Entry<String, Object> entry : job.getRun().getEnvironments().toMap().entrySet()) {
      text += "export " + entry.getKey().replace(' ', '_') + "=\"" + entry.getValue().toString().replace("\"", "\\\"") + "\"\n";
    }
    return text;
  }

  String xsubSubmitCommand(AbstractJob job) throws FailedToControlRemoteException, RunNotFoundException {
    return xsubCommand(job) + " " + BATCH_FILE;
  }

  String xsubCommand(AbstractJob job) throws FailedToControlRemoteException, RunNotFoundException {
    Computer computer = job.getComputer();
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(computer) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getRunDirectory(job.getRun()).toString() + "'; " +
      "XSUB_TYPE=$XSUB_TYPE $XSUB_COMMAND -p '" + computer.getXsubParameters().toString().replaceAll("'", "\\\\'") + "' ";
  }

  String xstatCommand(AbstractJob job) {
    Computer computer = job.getComputer();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + getXsubBinDirectory(computer) + "xstat " + job.getJobId();
  }

  String xdelCommand(AbstractJob job) {
    Computer computer = job.getComputer();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + getXsubBinDirectory(computer) + "xdel " + job.getJobId();
  }

  void processXsubSubmit(AbstractJob job, String json) throws Exception {
    try {
      JSONObject object = new JSONObject(json);
      String jobId = object.getString("job_id");
      job.setJobId(jobId);
      job.setState(State.Submitted);
      InfoLogMessage.issue(job.getRun(), "was submitted");
    } catch (Exception e) {
      throw e;
    }
  }

  void processXstat(AbstractJob job, String json) throws RunNotFoundException {
    InfoLogMessage.issue(job.getRun(), "will be checked");
    JSONObject object = null;
    try {
      object = new JSONObject(json);
    } catch (JSONException e) {
      WarnLogMessage.issue(e.getMessage() + json);
      job.setState(State.Excepted);
      return;
    }
    try {
      String status = object.getString("status");
      switch (status) {
        case "running" :
          job.setState(State.Running);
          break;
        case "finished" :
          job.setState(State.Finalizing);
          break;
      }
    } catch (Exception e) {
      job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
      ErrorLogMessage.issue(e);
    }
  }

  void jobFinalizing(AbstractJob job) throws WaffleException {
    try {
      int exitStatus = -1;
      try {
        exitStatus = Integer.parseInt(getFileContents(job.getRun(), getRunDirectory(job.getRun()).resolve(jp.tkms.waffle.sub.servant.Constants.EXIT_STATUS_FILE)).trim());
      } catch (Exception e) {
        job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
        WarnLogMessage.issue(e);
      }
      job.getRun().setExitStatus(exitStatus);

      Path runDirectoryPath = getRunDirectory(job.getRun());

      try {
        if (exists(runDirectoryPath.resolve(jp.tkms.waffle.sub.servant.Constants.STDOUT_FILE))) {
          transferFilesFromRemote(runDirectoryPath.resolve(jp.tkms.waffle.sub.servant.Constants.STDOUT_FILE),
            job.getRun().getDirectoryPath().resolve(jp.tkms.waffle.sub.servant.Constants.STDOUT_FILE));
        }
      } catch (Exception | Error e) {
        job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
        WarnLogMessage.issue(job.getRun(), "could not finds a remote " + jp.tkms.waffle.sub.servant.Constants.STDOUT_FILE);
      }

      try {
        if (exists(runDirectoryPath.resolve(jp.tkms.waffle.sub.servant.Constants.STDERR_FILE))) {
          transferFilesFromRemote(runDirectoryPath.resolve(jp.tkms.waffle.sub.servant.Constants.STDERR_FILE),
            job.getRun().getDirectoryPath().resolve(jp.tkms.waffle.sub.servant.Constants.STDERR_FILE));
        }
      } catch (Exception | Error e) {
        job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
        WarnLogMessage.issue(job.getRun(), "could not finds a remote " + jp.tkms.waffle.sub.servant.Constants.STDERR_FILE);
      }

      if (exitStatus == 0) {
        InfoLogMessage.issue(job.getRun(), "results will be collected");

        boolean isNoException = true;
        try {
          job.getRun().specializedPostProcess(this, job);
        } catch (Exception e) {
          isNoException = false;
          job.setState(State.Excepted);
          job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
          WarnLogMessage.issue(e);
        }

        if (isNoException) {
          job.setState(State.Finished);
        }
      } else {
        job.setState(State.Failed);
      }
      job.remove();
    } catch (Exception e) {
      try {
        job.getRun().appendErrorNote(LogMessage.getStackTrace(e));
      } catch (RunNotFoundException runNotFoundException) {
        ErrorLogMessage.issue(e);
        return;
      }
      ErrorLogMessage.issue(e);
    }
  }

  void processXdel(AbstractJob job, String json) throws RunNotFoundException {
    // nothing to do because the xdel does not need processing of its outputs currently
  }

  Path getContentsPath(ComputerTask run, Path path) throws FailedToControlRemoteException {
    if (path.isAbsolute()) {
      return path;
    }
    return getBaseDirectory(run).resolve(path);
  }

  public static String getXsubBinDirectory(Computer computer) {
    String separator = (computer.isLocal() ? File.separator : "/");
    return (computer.getXsubDirectory().equals("") ? "": computer.getXsubDirectory() + separator + "bin" + separator);
  }

  public static Path getWaffleServantPath(AbstractSubmitter submitter, Computer computer) throws FailedToControlRemoteException {
    return submitter.parseHomePath(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
  }

  public static boolean checkWaffleServant(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer).connect(retry);
    Path remoteServantPath = getWaffleServantPath(submitter, computer);
    boolean result = false;
    if (submitter.exists(remoteServantPath)) {
      result = true;
    } else {
      submitter.createDirectories(remoteServantPath.getParent());
      submitter.transferFilesToRemote(ServantJarFile.getPath(), remoteServantPath);
      result = submitter.exists(remoteServantPath);
    }
    submitter.close();
    return result;
  }

  public static JSONObject getXsubTemplate(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer).connect(retry);
    JSONObject jsonObject = new JSONObject();
    String command = "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE " +
      getXsubBinDirectory(computer) + "xsub -t";
    String json = submitter.exec(command);
    if (json != null) {
      try {
        jsonObject = new JSONObject(json);
      } catch (Exception e) {
        if (submitter.exec("which '" + getXsubBinDirectory(computer) + "xsub' 2>/dev/null; if test 0 -ne $?; then echo NotFound; fi;").startsWith("NotFound")) {
          submitter.close();
          throw new NotFoundXsubException(e);
        }
        String message =
          submitter.exec("if test ! -e '" + getXsubBinDirectory(computer) + "xsub'; then echo NotFound; fi;");
        submitter.close();
        throw new RuntimeException("Failed to parse JSON : " + message );
      }
    }
    submitter.close();
    return jsonObject;
  }

  public static JSONObject getParameters(Computer computer) {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer);
    JSONObject jsonObject = submitter.getDefaultParameters(computer);
    return jsonObject;
  }

  protected boolean isSubmittable(Computer computer, AbstractJob job) {
    return isSubmittable(computer, job, getJobList(PollingThread.Mode.Normal, computer));
  }

  protected boolean isSubmittable(Computer computer, AbstractJob next, ArrayList<AbstractJob>... lists) {
    ArrayList<AbstractJob> combinedList = new ArrayList<>();
    for (ArrayList<AbstractJob> list : lists) {
      combinedList.addAll(list);
    }
    return isSubmittable(computer, next, combinedList);
  }

  protected boolean isSubmittable(Computer computer, AbstractJob next, ArrayList<AbstractJob> list) {
    ComputerTask nextRun = null;
    try {
      if (next != null) {
        nextRun = next.getRun();
      }
    } catch (RunNotFoundException e) {
    }

    return (list.size() + (nextRun == null ? 0 : 1)) <= computer.getMaximumNumberOfJobs();
  }

  protected static ArrayList<AbstractJob> getJobList(PollingThread.Mode mode, Computer computer) {
    if (mode.equals(PollingThread.Mode.Normal)) {
      return ExecutableRunJob.getList(computer);
    } else {
      return SystemTaskJob.getList(computer);
    }
  }

  /*
  public void pollingTask() throws FailedToControlRemoteException {
    pollingInterval = computer.getPollingInterval();
    ArrayList<AbstractJob> jobList = getJobList(mode, computer);

    createdJobList.clear();
    preparedJobList.clear();
    submittedJobList.clear();
    runningJobList.clear();
    cancelJobList.clear();



    for (AbstractJob job : jobList) {
      try {
        if (!job.exists() && job.getRun().isRunning()) {
          job.cancel();
          WarnLogMessage.issue(job.getRun(), "The task file is not exists; The task will cancel.");
          continue;
        }
        switch (job.getState(true)) {
          case Created:
            if (isSubmittable(computer, null, createdJobList, preparedJobList)) {
              job.getRun(); // check exists
              createdJobList.add(job);
            }
            break;
          case Prepared:
            if (isSubmittable(computer, null, createdJobList, preparedJobList)) {
              job.getRun(); // check exists
              preparedJobList.add(job);
            }
            break;
          case Submitted:
            submittedJobList.add(job);
            break;
          case Running:
            runningJobList.add(job);
            break;
          case Cancel:
            cancelJobList.add(job);
            break;
          case Finalizing:
            WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") was under finalizing when system stopped; The task was removed." );
          case Finished:
          case Failed:
          case Excepted:
          case Canceled:
            job.remove();
        }
      } catch (RunNotFoundException e) {
        try {
          cancel(job);
        } catch (RunNotFoundException ex) { }
        job.remove();
        WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
      }

      if (Main.hibernateFlag) { break; }
    }

    processJobLists(createdJobList, preparedJobList, submittedJobList, runningJobList, cancelJobList);
  }
   */

  /*
  public double getMaximumNumberOfThreads(Computer computer) {
    return computer.getMaximumNumberOfThreads();
  }

  public double getAllocableMemorySize(Computer computer) {
    return computer.getAllocableMemorySize();
  }
   */

  /*
  public void processJobLists(ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList, ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> runningJobList, ArrayList<AbstractJob> cancelJobList) throws FailedToControlRemoteException {
    //int submittedCount = submittedJobList.size() + runningJobList.size();
    submittedJobList.addAll(runningJobList);
    ArrayList<AbstractJob> submittedJobListForAggregation = new ArrayList<>(submittedJobList);
    ArrayList<AbstractJob> queuedJobList = new ArrayList<>();
    queuedJobList.addAll(preparedJobList);
    queuedJobList.addAll(createdJobList);

    for (AbstractJob job : cancelJobList) {
      try {
        cancel(job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }
    cacheClear();

    ArrayList<Future> futureList = new ArrayList<>();
    int limiter = 10;
    for (AbstractJob job : createdJobList) {
      if (--limiter < 0) {
        break;
      }
      futureList.add(threadPool.submit(() -> {
        try {
          prepareJob(job);
        } catch (FailedToControlRemoteException e) {
          WarnLogMessage.issue(job.getComputer(), e.getMessage());
        } catch (WaffleException e) {
          WarnLogMessage.issue(e);
        }
      }));
    }

    for (Future future : futureList) {
      try {
        future.get();
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    }
    cacheClear();

    for (AbstractJob job : submittedJobList) {
      if (Main.hibernateFlag) { break; }

      try {
        switch (update(job)) {
          case Finished:
          case Failed:
          case Excepted:
          case Canceled:
            submittedJobListForAggregation.remove(job);
            if (! queuedJobList.isEmpty()) {
              AbstractJob nextJob = queuedJobList.get(0);
              if (isSubmittable(computer, nextJob, submittedJobListForAggregation)) {
                submit(nextJob);
                queuedJobList.remove(nextJob);
                submittedJobListForAggregation.add(nextJob);
              }
            }
        }
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }
    cacheClear();

    for (AbstractJob job : queuedJobList) {
      if (Main.hibernateFlag) { break; }

      try {
        if (isSubmittable(computer, job, submittedJobListForAggregation)) {
          submit(job);
          submittedJobListForAggregation.add(job);
        }
      } catch (NullPointerException | WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }
    cacheClear();
  }
   */

  protected void processEnvelope(Envelope envelope) {
    try {
      Envelope response = sendAndReceiveEnvelope(envelope);
    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }
  }

  public void checkSubmitted() throws FailedToControlRemoteException {
    isRunning = true;
    pollingInterval = computer.getPollingInterval();

    createdProcessorManager.startup();
    preparedProcessorManager.startup();
    finishedProcessorManager.startup();

    ArrayList<AbstractJob> submittedJobList = new ArrayList<>();
    ArrayList<AbstractJob> runningJobList = new ArrayList<>();
    ArrayList<AbstractJob> cancelJobList = new ArrayList<>();

    for (AbstractJob job : new ArrayList<>(getJobList(mode, computer))) {
      if (Main.hibernateFlag) { return; }

      try {
        if (!job.exists() && job.getRun().isRunning()) {
          job.cancel();
          WarnLogMessage.issue(job.getRun(), "The task file is not exists; The task will cancel.");
          continue;
        }
        switch (job.getState(true)) {
          case Submitted:
            submittedJobList.add(job);
            break;
          case Running:
            runningJobList.add(job);
            break;
          case Cancel:
            cancelJobList.add(job);
        }
      } catch (RunNotFoundException e) {
        try {
          cancel(job);
        } catch (RunNotFoundException ex) { }
        job.remove();
        WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
      }
    }

    if (!Main.hibernateFlag) {
      processSubmitted(submittedJobList, runningJobList, cancelJobList);
    }

    if (!Main.hibernateFlag) {
      preparedProcessorManager.startup();
    }

    isRunning = false;
    return;
  }

  public void processSubmitted(ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> runningJobList, ArrayList<AbstractJob> cancelJobList) throws FailedToControlRemoteException {
    //int submittedCount = submittedJobList.size() + runningJobList.size();
    submittedJobList.addAll(runningJobList);

    for (AbstractJob job : cancelJobList) {
      if (Main.hibernateFlag) { return; }
      try {
        cancel(job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    for (AbstractJob job : submittedJobList) {
      if (Main.hibernateFlag) { return; }

      try {
        switch (update(job)) {
          case Finalizing:
          case Finished:
          case Failed:
          case Excepted:
          case Canceled:
            finishedProcessorManager.startup();
            preparedProcessorManager.startup();
        }
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }
  }

  class CreatedProcessor extends Thread {
    @Override
    public void run() {
      ArrayList<AbstractJob> createdJobList = new ArrayList<>();
      ArrayList<AbstractJob> preparedJobList = new ArrayList<>();

      do {
        if (Main.hibernateFlag) {
          return;
        }

        createdJobList.clear();
        preparedJobList.clear();

        for (AbstractJob job : new ArrayList<>(getJobList(mode, computer))) {
          try {
            if (!job.exists() && job.getRun().isRunning()) {
              job.cancel();
              WarnLogMessage.issue(job.getRun(), "The task file is not exists; The task will cancel.");
              continue;
            }
            switch (job.getState(true)) {
              case Created:
                createdJobList.add(job);
                break;
              case Prepared:
                preparedJobList.add(job);
            }
          } catch (RunNotFoundException e) {
            try {
              cancel(job);
            } catch (RunNotFoundException ex) {
            }
            job.remove();
            WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed.");
          }
        }

        if (createdJobList.isEmpty() || preparedJobList.size() >= preparingNumber || Main.hibernateFlag) {
          return;
        }

        try {
          processCreated(createdJobList, preparedJobList);
        } catch (FailedToControlRemoteException e) {
          ErrorLogMessage.issue(e);
        }
      } while (!createdJobList.isEmpty() && preparedJobList.size() < preparingNumber);
      return;
    }
  }

  public void processCreated(ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    int preparedCount = preparedJobList.size();
    /*
    ArrayList<Future> futures = new ArrayList<>();
    for (AbstractJob job : createdJobList) {
      if (Main.hibernateFlag || preparedCount >= preparingNumber) {
        break;
      }

      futures.add(threadPool.submit(() -> {
        try {
          prepareJob(job);
        } catch (FailedToTransferFileException e) {
          WarnLogMessage.issue(job.getComputer(), e.getMessage());
        } catch (WaffleException e) {
          WarnLogMessage.issue(e);
        }
      }));
      preparedCount += 1;
    }

    for (Future future : futures) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        ErrorLogMessage.issue(e);
      }
    }
    */

    Envelope envelope = new Envelope(Constants.WORK_DIR);
    for (AbstractJob job : createdJobList) {
      if (Main.hibernateFlag || preparedCount >= preparingNumber) {
        break;
      }

      try {
        prepareJob(envelope, job);
      } catch (FailedToTransferFileException e) {
        WarnLogMessage.issue(job.getComputer(), e.getMessage());
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
      }

      preparedCount += 1;
    }

    processEnvelope(envelope);

    return;
  }

  class PreparedProcessor extends Thread {
    @Override
    public void run() {
      long s = System.currentTimeMillis();
      createdProcessorManager.startup();

      ArrayList<AbstractJob> submittedJobList = new ArrayList<>();
      ArrayList<AbstractJob> createdJobList = new ArrayList<>();
      ArrayList<AbstractJob> preparedJobList = new ArrayList<>();

      for (AbstractJob job : new ArrayList<>(getJobList(mode, computer))) {
        if (Main.hibernateFlag) { return; }
        try {
          if (!job.exists() && job.getRun().isRunning()) {
            job.cancel();
            WarnLogMessage.issue(job.getRun(), "The task file is not exists; The task will cancel.");
            continue;
          }
          switch (job.getState(true)) {
            case Created:
              createdJobList.add(job);
              break;
            case Prepared:
              preparedJobList.add(job);
              break;
            case Submitted:
            case Running:
            case Cancel:
              submittedJobList.add(job);
          }
        } catch (RunNotFoundException e) {
          try {
            cancel(job);
          } catch (RunNotFoundException ex) { }
          job.remove();
          WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
        }
      }

      if (!Main.hibernateFlag) {
        try {
          processPrepared(submittedJobList, createdJobList, preparedJobList);
        } catch (FailedToControlRemoteException e) {
          ErrorLogMessage.issue(e);
        }
      }
      return;
    }
  }

  public void processPrepared(ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    ArrayList<AbstractJob> queuedJobList = new ArrayList<>();
    queuedJobList.addAll(preparedJobList);
    queuedJobList.addAll(createdJobList);

    Envelope envelope = new Envelope(Constants.WORK_DIR);

    for (AbstractJob job : queuedJobList) {
      if (Main.hibernateFlag) { return; }

      try {
        if (isSubmittable(computer, job, submittedJobList)) {
          submit(envelope, job);
          submittedJobList.add(job);
          createdProcessorManager.startup();
        }
      } catch (NullPointerException | WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }
  }

  class FinishedProcessor extends Thread {
    @Override
    public void run() {
      ArrayList<AbstractJob> finalizingJobList = new ArrayList<>();

      do {
        if (Main.hibernateFlag) { return; }

        finalizingJobList.clear();

        for (AbstractJob job : new ArrayList<>(getJobList(mode, computer))) {
          if (Main.hibernateFlag) { return; }

          try {
            if (!job.exists() && job.getRun().isRunning()) {
              job.cancel();
              WarnLogMessage.issue(job.getRun(), "The task file is not exists; The task will cancel.");
              continue;
            }
            switch (job.getState(true)) {
              case Finalizing:
                finalizingJobList.add(job);
                break;
              case Finished:
              case Failed:
              case Excepted:
                //WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not running; The task was removed." );
              case Canceled:
                job.remove();
            }
          } catch (RunNotFoundException e) {
            try {
              cancel(job);
            } catch (RunNotFoundException ex) { }
            job.remove();
            WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
          }
        }

        if (finalizingJobList.isEmpty()) {
          return;
        }

        try {
          processFinished(finalizingJobList);
        } catch (FailedToControlRemoteException e) {
          ErrorLogMessage.issue(e);
        }

      } while (!finalizingJobList.isEmpty());
      return;
    }
  }

  public void processFinished(ArrayList<AbstractJob> finalizingJobList) throws FailedToControlRemoteException {
    for (AbstractJob job : finalizingJobList) {
      if (Main.hibernateFlag) { return; }

      try {
        jobFinalizing(job);
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }
  }

  static class ProcessorManager<P extends Thread> {
    Supplier<P> supplier;
    P processor;

    ProcessorManager(Supplier<P> supplier) {
      this.supplier = supplier;
      processor = null;
    }

    protected void startup() {
      if (processor == null || !processor.isAlive()) {
        synchronized (this) {
          if (processor == null || !processor.isAlive()) {
            processor = supplier.get();
            processor.start();
          }
        }
      }
    }

    protected void close() {
      if (processor != null && processor.isAlive()) {
        int count = 0;
        while (processor.isAlive()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            ErrorLogMessage.issue(e);
          }
          if (count++ == 10) {
            InfoLogMessage.issue("Submitter is closing; wait few minutes.");
          }
        }
      }
    }
  }

  public void close() {
    synchronized (this) {
      while (isRunning) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          ErrorLogMessage.issue(e);
        }
      }
      preparedProcessorManager.close();
      createdProcessorManager.close();
      finishedProcessorManager.close();
    }
  }
}
