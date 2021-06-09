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
import jp.tkms.waffle.data.job.SystemTaskStore;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.*;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.TaskJson;
import jp.tkms.waffle.sub.servant.message.request.*;
import jp.tkms.waffle.sub.servant.message.response.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

abstract public class AbstractSubmitter {
  protected static final String DOT_ENVELOPE = ".ENVELOPE";
  protected static final String TASK_JSON = "task.json";
  protected static final String RUN_DIR = "run";
  protected static final String BATCH_FILE = "batch.sh";

  boolean isRunning = false;
  Computer computer;
  private int pollingInterval = 5;
  private int preparingNumber = 1;
  private PollingThread.Mode mode;

  //ProcessorManager<CreatedProcessor> createdProcessorManager = new ProcessorManager<>(() -> new CreatedProcessor());
  ProcessorManager<PreparingProcessor> preparingProcessorManager = new ProcessorManager<>(() -> new PreparingProcessor());
  ProcessorManager<FinishedProcessor> finishedProcessorManager = new ProcessorManager<>(() -> new FinishedProcessor());

  private static Path tempDirectoryPath = null;

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
  abstract boolean deleteFile(Path path) throws FailedToControlRemoteException;
  abstract public String exec(String command) throws FailedToControlRemoteException;
  //abstract public void putText(AbstractJob job, Path path, String text) throws FailedToTransferFileException, RunNotFoundException;
  abstract public String getFileContents(ComputerTask run, Path path) throws FailedToTransferFileException;
  abstract public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException;
  abstract public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException;

  public AbstractSubmitter(Computer computer) {
    this.computer = computer;
  }

  public AbstractSubmitter connect() {
    return connect(true);
  }

  /*
  public void putText(AbstractJob job, String pathString, String text) throws FailedToTransferFileException, RunNotFoundException {
    putText(job, Paths.get(pathString), text);
  }
   */

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

  private static Path getTempDirectoryPath() throws IOException {
    if (tempDirectoryPath == null) {
      synchronized (AbstractSubmitter.class) {
        if (tempDirectoryPath == null) {
          tempDirectoryPath = Files.createTempDirectory(Constants.APP_NAME);
          tempDirectoryPath.toFile().deleteOnExit();
        }
      }
    }
    return tempDirectoryPath;
  }

  public static Envelope sendAndReceiveEnvelope(AbstractSubmitter submitter, Envelope envelope) throws Exception {
    //envelope.getMessageBundle().print("HOST");
    if (!envelope.isEmpty()) {
      Path tmpFile = getTempDirectoryPath().resolve(UUID.randomUUID().toString());
      Files.createDirectories(tmpFile.getParent());
      envelope.save(tmpFile);
      Path remoteWorkBasePath = submitter.parseHomePath(submitter.computer.getWorkBaseDirectory());
      Path remoteEnvelopePath = remoteWorkBasePath.resolve(DOT_ENVELOPE).resolve(tmpFile.getFileName());
      submitter.createDirectories(remoteEnvelopePath.getParent());
      submitter.transferFilesToRemote(tmpFile, remoteEnvelopePath);
      Files.delete(tmpFile);

      System.out.print(submitter.exec("java --illegal-access=deny --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED -jar '" + getWaffleServantPath(submitter, submitter.computer)
        + "' '" + remoteWorkBasePath + "' main '" + remoteEnvelopePath + "'"));
      Path remoteResponsePath = Envelope.getResponsePath(remoteEnvelopePath);
      submitter.transferFilesFromRemote(remoteResponsePath, tmpFile);
      submitter.deleteFile(remoteResponsePath);
      Envelope response = Envelope.loadAndExtract(Constants.WORK_DIR, tmpFile);
      Files.delete(tmpFile);
      return response;
    }
    return null;
  }

  public Envelope sendAndReceiveEnvelope(Envelope envelope) throws Exception {
    return sendAndReceiveEnvelope(this, envelope);
  }

  public void forcePrepare(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    if (job.getState().equals(State.Created)) {
      prepareJob(envelope, job);
    }
  }

  public void submit(Envelope envelope, AbstractJob job) throws RunNotFoundException {
    try {
      forcePrepare(envelope, job);
      envelope.add(new SubmitJobMessage(job.getTypeCode(), job.getHexCode(), getRunDirectory(job.getRun()), job.getRun().getRemoteBinPath(), BATCH_FILE, computer.getXsubParameters().toString()));
    } catch (FailedToControlRemoteException e) {
      WarnLogMessage.issue(job.getComputer(), e.getMessage());
      job.setState(State.Excepted);
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      job.setState(State.Excepted);
    }
  }

  public void update(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException {
    envelope.add(new CollectStatusMessage(job.getTypeCode(), job.getHexCode(), job.getJobId(), getRunDirectory(job.getRun())));
  }

  public void cancel(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException {
    if (! job.getJobId().equals("-1")) {
      envelope.add(new CancelJobMessage(job.getTypeCode(), job.getHexCode(), job.getJobId(), getRunDirectory(job.getRun())));
    } else {
      job.setState(State.Canceled);
    }
  }

  protected void prepareJob(Envelope envelope, AbstractJob job) throws RunNotFoundException, FailedToControlRemoteException,FailedToTransferFileException {
    synchronized (job) {
      if (job.getState().equals(State.Created)) {
        ComputerTask run = job.getRun();
        run.setRemoteWorkingDirectoryLog(getRunDirectory(run).toString());

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

        Path remoteBinPath = run.getRemoteBinPath();
        TaskJson taskJson = new TaskJson(projectName, remoteBinPath == null ? null : remoteBinPath.toString(), run.getCommand(),
          arguments, environments, localShared);
        //putText(job, TASK_JSON, taskJson.toString());
        envelope.add(new PutTextFileMessage(run.getLocalDirectoryPath().resolve(TASK_JSON), taskJson.toString()));

        //putText(job, BATCH_FILE, "java -jar '" + getWaffleServantPath(this, computer) + "' '" + parseHomePath(computer.getWorkBaseDirectory()) + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'");
        envelope.add(new PutTextFileMessage(run.getLocalDirectoryPath().resolve(BATCH_FILE), "java -jar '" + getWaffleServantPath()
          + "' '" + getWorkBaseDirectory() + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'"));
        //+ "' '" + parseHomePath(computer.getWorkBaseDirectory()) + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'"));

        Path remoteExecutableBaseDirectory = getExecutableBaseDirectory(job);
        if (remoteExecutableBaseDirectory != null && !exists(remoteExecutableBaseDirectory.toAbsolutePath())) {
          envelope.add(run.getBinPath());
        }

        envelope.add(run.getBasePath());

        job.setState(State.Prepared);
        InfoLogMessage.issue(job.getRun(), "was prepared");
      }
    }
  }

  public Path getWorkBaseDirectory() {
    return Paths.get(computer.getWorkBaseDirectory());
  }

  public Path getBaseDirectory(ComputerTask run) throws FailedToControlRemoteException {
    return getRunDirectory(run).resolve(Executable.BASE);
  }

  public Path getRunDirectory(ComputerTask run) throws FailedToControlRemoteException {
    //Computer computer = run.getActualComputer();
    //Path path = parseHomePath(computer.getWorkBaseDirectory()).resolve(run.getLocalDirectoryPath());
    Path path = parseHomePath(getWorkBaseDirectory().toString()).resolve(run.getLocalDirectoryPath());
    createDirectories(path);
    return path;
  }

  Path getExecutableBaseDirectory(AbstractJob job) throws FailedToControlRemoteException, RunNotFoundException {
    Path remoteBinPath = job.getRun().getRemoteBinPath();
    if (remoteBinPath == null) {
      return null;
    } else {
      return parseHomePath(job.getComputer().getWorkBaseDirectory()).resolve(remoteBinPath);
    }
  }

  void jobFinalizing(AbstractJob job) throws WaffleException {
    try {
      int exitStatus = job.getRun().getExitStatus();

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

  Path getContentsPath(ComputerTask run, Path path) throws FailedToControlRemoteException {
    if (path.isAbsolute()) {
      return path;
    }
    return getBaseDirectory(run).resolve(path);
  }

  public static Path getWaffleServantPath(AbstractSubmitter submitter, Computer computer) throws FailedToControlRemoteException {
    //return submitter.parseHomePath(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
    return Paths.get(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
  }

  public Path getWaffleServantPath() throws FailedToControlRemoteException {
    //return submitter.parseHomePath(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
    return getWaffleServantPath(this, computer);
  }

  public static boolean checkWaffleServant(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer);
    if (submitter instanceof AbstractSubmitterWrapper) {
      return false;
    }
    submitter.connect(retry);
    Path remoteServantPath = getWaffleServantPath(submitter, computer);
    boolean result = false;

    if (submitter.exists(remoteServantPath)) {
      String remoteHash = submitter.exec("md5sum '" + remoteServantPath + "'  | sed -e 's/ .*//'").trim(); //TODO:
      String localHash = ServantJarFile.getMD5Sum().trim();
      if (remoteHash.length() == localHash.length() && !remoteHash.equals(localHash)) {
        submitter.deleteFile(remoteServantPath);
      } else {
        result = true;
      }
    }

    if (!result) {
      submitter.createDirectories(remoteServantPath.getParent());
      submitter.transferFilesToRemote(ServantJarFile.getPath(), remoteServantPath);
      result = submitter.exists(remoteServantPath);
    }
    submitter.close();
    return result;
  }

  public static JSONObject getXsubTemplate(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer);
    JSONObject jsonObject = new JSONObject();
    if (!(submitter instanceof AbstractSubmitterWrapper)) {
      submitter.connect(retry);
      Envelope request = new Envelope(Constants.WORK_DIR);
      request.add(new SendXsubTemplateMessage());
      try {
        Envelope response = submitter.sendAndReceiveEnvelope(request);
        for (XsubTemplateMessage message : response.getMessageBundle().getCastedMessageList(XsubTemplateMessage.class)) {
          jsonObject = new JSONObject(message.getTemplate());
          break;
        }
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      submitter.close();
    }
    return jsonObject;
  }

  public static JSONObject getParameters(Computer computer) {
    AbstractSubmitter submitter = getInstance(PollingThread.Mode.Normal, computer);
    JSONObject jsonObject = submitter.getDefaultParameters(computer);
    return jsonObject;
  }

  protected boolean isSubmittable(Computer computer, ComputerTask next) {
    return isSubmittable(computer, next, getJobList(PollingThread.Mode.Normal, computer), getJobList(PollingThread.Mode.System, computer));
  }

  protected boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<AbstractJob>... lists) {
    ArrayList<ComputerTask> combinedList = new ArrayList<>();
    for (ArrayList<AbstractJob> list : lists) {
      for (AbstractJob job : list) {
        try {
          combinedList.add(job.getRun());
        } catch (RunNotFoundException e) {
          //NOP
        }
      }
    }
    return isSubmittable(computer, next, combinedList);
  }

  protected boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<ComputerTask> list) {
    return (list.size() + (next == null ? 0 : 1)) <= computer.getMaximumNumberOfJobs();
  }

  protected static ArrayList<AbstractJob> getJobList(PollingThread.Mode mode, Computer computer) {
    if (mode.equals(PollingThread.Mode.Normal)) {
      return ExecutableRunJob.getList(computer);
    } else {
      return SystemTaskJob.getList(computer);
    }
  }

  protected static AbstractJob findJobFromStore(byte type, String id) {
    WaffleId targetId = WaffleId.valueOf(id);
    if (type == SystemTaskStore.TYPE_CODE) {
      for (SystemTaskJob job : SystemTaskJob.getList()) {
        if (job.getId().equals(targetId)) {
          return job;
        }
      }
    } else {
      for (ExecutableRunJob job : ExecutableRunJob.getList()) {
        if (job.getId().equals(targetId)) {
          return job;
        }
      }
    }
    return null;
  }

  protected Envelope processRequestAndResponse(Envelope envelope) {
    Envelope response = null;
    try {
      response = sendAndReceiveEnvelope(envelope);

      if (response == null) {
        return null;
      }

      for (ExceptionMessage message : response.getMessageBundle().getCastedMessageList(ExceptionMessage.class)) {
        ErrorLogMessage.issue("Servant> " + message.getMessage());
      }

      for (JobExceptionMessage message : response.getMessageBundle().getCastedMessageList(JobExceptionMessage.class)) {
        WarnLogMessage.issue("Servant:JobException> " + message.getMessage());

        AbstractJob job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setState(State.Excepted);
        }
      }

      for (UpdateResultMessage message : response.getMessageBundle().getCastedMessageList(UpdateResultMessage.class)) {
        AbstractJob job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          if (job instanceof ExecutableRunJob) {
            ExecutableRun run = ((ExecutableRunJob) job).getRun();
            Object value = message.getValue();
            try {
              if (message.getValue().indexOf('.') < 0) {
                value = Integer.valueOf(message.getValue());
              } else {
                value = Double.valueOf(message.getValue());
              }
            } catch (Exception e) {
              if (message.getValue().equalsIgnoreCase("true")) {
                value = Boolean.TRUE;
              } else if (message.getValue().equalsIgnoreCase("false")) {
                value = Boolean.FALSE;
              }
            }
            run.putResultDynamically(message.getKey(), value);
          }
        }
      }

      for (JobCanceledMessage message : response.getMessageBundle().getCastedMessageList(JobCanceledMessage.class)) {
        AbstractJob job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setState(State.Canceled);
        }
      }

      for (UpdateJobIdMessage message : response.getMessageBundle().getCastedMessageList(UpdateJobIdMessage.class)) {
        AbstractJob job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setJobId(message.getJobId());
          job.setState(State.Submitted);
          job.getRun().setRemoteWorkingDirectoryLog(message.getWorkingDirectory());
          InfoLogMessage.issue(job.getRun(), "was submitted");
        }
      }

      for (UpdateStatusMessage message : response.getMessageBundle().getCastedMessageList(UpdateStatusMessage.class)) {
        AbstractJob job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          if (message.isFinished()) {
            job.setState(State.Finalizing);
            job.getRun().setExitStatus(message.getExitStatus());
            finishedProcessorManager.startup();
            preparingProcessorManager.startup();
          } else {
            if (job.getState().equals(State.Submitted)) {
              job.setState(State.Running);
            }
          }
        }
      }

    } catch (Exception e) {
      ErrorLogMessage.issue(e);
    }

    return response;
  }

  public void checkSubmitted() throws FailedToControlRemoteException {
    Envelope envelope = new Envelope(Constants.WORK_DIR);

    isRunning = true;
    pollingInterval = computer.getPollingInterval();

    //createdProcessorManager.startup();
    preparingProcessorManager.startup();
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
          cancel(envelope, job);
        } catch (RunNotFoundException ex) { }
        job.remove();
        WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
      }
    }

    if (!Main.hibernateFlag) {
      processSubmitted(envelope, submittedJobList, runningJobList, cancelJobList);
    }

    if (!Main.hibernateFlag) {
      preparingProcessorManager.startup();
    }

    isRunning = false;

    processRequestAndResponse(envelope);

    return;
  }

  public void processSubmitted(Envelope envelope, ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> runningJobList, ArrayList<AbstractJob> cancelJobList) throws FailedToControlRemoteException {
    submittedJobList.addAll(runningJobList);

    for (AbstractJob job : cancelJobList) {
      if (Main.hibernateFlag) { return; }
      try {
        cancel(envelope, job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    for (AbstractJob job : submittedJobList) {
      if (Main.hibernateFlag) { return; }
      try {
        update(envelope, job);
      } catch (RunNotFoundException e) {
        job.remove();
        WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
      }
    }
  }

  /*
  class CreatedProcessor extends Thread {
    @Override
    public void run() {
      ArrayList<AbstractJob> createdJobList = new ArrayList<>();
      ArrayList<AbstractJob> preparedJobList = new ArrayList<>();

      do {
        if (Main.hibernateFlag) {
          return;
        }

        Envelope envelope = new Envelope(Constants.WORK_DIR);

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
              cancel(envelope, job);
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
          processCreated(envelope, createdJobList, preparedJobList);
        } catch (FailedToControlRemoteException e) {
          ErrorLogMessage.issue(e);
        }

        processRequestAndResponse(envelope);
      } while (!createdJobList.isEmpty() && preparedJobList.size() < preparingNumber);
      return;
    }
  }

  public void processCreated(Envelope envelope, ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    int preparedCount = preparedJobList.size();

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

    return;
  }
   */

  class PreparingProcessor extends Thread {
    @Override
    public void run() {
      //createdProcessorManager.startup();

      Envelope envelope = new Envelope(Constants.WORK_DIR);

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
            cancel(envelope, job);
          } catch (RunNotFoundException | FailedToControlRemoteException ex) { }
          job.remove();
          WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
        }
      }

      if (!Main.hibernateFlag) {
        try {
          processPreparing(envelope, submittedJobList, createdJobList, preparedJobList);
        } catch (FailedToControlRemoteException e) {
          ErrorLogMessage.issue(e);
        }
      }

      processRequestAndResponse(envelope);

      return;
    }
  }

  public void processPreparing(Envelope envelope, ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    ArrayList<AbstractJob> queuedJobList = new ArrayList<>();
    queuedJobList.addAll(preparedJobList);
    queuedJobList.addAll(createdJobList);

    boolean givePenalty = false;

    for (AbstractJob job : queuedJobList) {
      if (Main.hibernateFlag) { return; }

      try {
        if (isSubmittable(computer, job.getRun(), submittedJobList)) {
          if (job.getState().equals(State.Created)) {
            givePenalty = true;
          }
          submit(envelope, job);
          submittedJobList.add(job);
          //createdProcessorManager.startup();
        }
      } catch (NullPointerException | WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }

    if (givePenalty) {
      preparingNumber += 1;
    }

    int preparedCount = preparedJobList.size();

    for (AbstractJob job : createdJobList) {
      if (Main.hibernateFlag || preparedCount >= preparingNumber) {
        break;
      }

      if (!submittedJobList.contains(job)) {
        try {
          prepareJob(envelope, job);
        } catch (FailedToTransferFileException e) {
          WarnLogMessage.issue(job.getComputer(), e.getMessage());
        } catch (WaffleException e) {
          WarnLogMessage.issue(e);
        }
      }

      preparedCount += 1;
    }
  }

  class FinishedProcessor extends Thread {
    @Override
    public void run() {
      ArrayList<AbstractJob> finalizingJobList = new ArrayList<>();

      do {
        if (Main.hibernateFlag) { return; }

        Envelope envelope = new Envelope(Constants.WORK_DIR);

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
              cancel(envelope, job);
            } catch (RunNotFoundException | FailedToControlRemoteException ex) { }
            job.remove();
            WarnLogMessage.issue("ExecutableRun(" + job.getId() + ") is not found; The task was removed." );
          }
        }

        if (!finalizingJobList.isEmpty()) {
          try {
            processFinished(finalizingJobList);
          } catch (FailedToControlRemoteException e) {
            ErrorLogMessage.issue(e);
          }
        }

        processRequestAndResponse(envelope);

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
      preparingProcessorManager.close();
      //createdProcessorManager.close();
      finishedProcessorManager.close();
    }
  }
}
