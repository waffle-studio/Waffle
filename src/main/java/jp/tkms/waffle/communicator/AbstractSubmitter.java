package jp.tkms.waffle.communicator;

import com.eclipsesource.json.JsonArray;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.communicator.annotation.CommunicatorDescription;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.inspector.Inspector;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.ServantJarFile;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.ExecutableRunTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.data.internal.task.SystemTaskStore;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.LogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.executable.Executable;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.*;
import jp.tkms.waffle.manager.Filter;
import jp.tkms.waffle.sub.servant.Envelope;
import jp.tkms.waffle.sub.servant.TaskJson;
import jp.tkms.waffle.sub.servant.message.request.*;
import jp.tkms.waffle.sub.servant.message.response.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

@CommunicatorDescription("ABSTRACT SUBMITTER")
abstract public class AbstractSubmitter {
  protected static final String DOT_ENVELOPE = ".ENVELOPE";
  protected static final String TASK_JSON = "task.json";
  protected static final String RUN_DIR = "run";
  protected static final String BATCH_FILE = "batch.sh";
  static final int INITIAL_PREPARING = 500;

  boolean isRunning = false;
  Computer computer;
  private int pollingInterval = 5;
  private int preparingSize = INITIAL_PREPARING;
  private Inspector.Mode mode;


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

  abstract public WrappedJson getDefaultParameters(Computer computer);

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

  public static AbstractSubmitter getInstance(Inspector.Mode mode, Computer computer) {
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

      String jvmActivationCommand = submitter.computer.getJvmActivationCommand().replace("\"", "\\\"");
      if (!jvmActivationCommand.trim().equals("") && !jvmActivationCommand.trim().endsWith(";")) {
        jvmActivationCommand += ";";
      }

      String message = (submitter.exec(
        getSanitizedEnvironments(submitter.computer)
        + "sh -c \"" + jvmActivationCommand
        + "java --illegal-access=deny --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED -jar '"
        + getWaffleServantPath(submitter, submitter.computer)
        + "' '" + remoteWorkBasePath + "' main '" + remoteEnvelopePath + "'\"")).trim();
      if (!"".equals(message)) {
        InfoLogMessage.issue(message);
      }
      Path remoteResponsePath = Envelope.getResponsePath(remoteEnvelopePath);
      submitter.transferFilesFromRemote(remoteResponsePath, tmpFile);
      submitter.deleteFile(remoteResponsePath);
      Envelope response = Envelope.loadAndExtract(Constants.WORK_DIR, tmpFile);
      Files.delete(tmpFile);
      return response;
    }
    return null;
  }

  public static String getSanitizedEnvironments(Computer computer) {
    String environments = "";
    for (Object key : computer.getEnvironments().keySet()) {
      String name = key.toString().trim().replace("\"", "\\\"").replaceAll(" |#|;", "_");
      String value = computer.getEnvironments().getString(name, "\"\"").replace("\\", "\\\\").replace("\"", "\\\"");
      environments += name + "=\"" + value + "\" ";
    }
    return environments;
  }

  public Envelope sendAndReceiveEnvelope(Envelope envelope) throws Exception {
    return sendAndReceiveEnvelope(this, envelope);
  }

  public void forcePrepare(Envelope envelope, AbstractTask job) throws RunNotFoundException, FailedToControlRemoteException, FailedToTransferFileException {
    if (job.getState().equals(State.Created)) {
      prepareJob(envelope, job);
    }
  }

  public void submit(Envelope envelope, AbstractTask job) throws RunNotFoundException {
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

  public void update(Envelope envelope, AbstractTask job) throws RunNotFoundException, FailedToControlRemoteException {
    envelope.add(new CollectStatusMessage(job.getTypeCode(), job.getHexCode(), job.getJobId(), getRunDirectory(job.getRun())));
  }

  public void cancel(Envelope envelope, AbstractTask job) throws RunNotFoundException, FailedToControlRemoteException {
    if (! job.getJobId().equals("-1")) {
      envelope.add(new CancelJobMessage(job.getTypeCode(), job.getHexCode(), job.getJobId(), getRunDirectory(job.getRun())));
    } else {
      job.setState(State.Canceled);
    }
  }

  protected void prepareJob(Envelope envelope, AbstractTask job) throws RunNotFoundException, FailedToControlRemoteException,FailedToTransferFileException {
    synchronized (job) {
      if (job.getState().equals(State.Created)) {
        ComputerTask run = job.getRun();
        run.setRemoteWorkingDirectoryLog(getRunDirectory(run).toString());

        String projectName = (run instanceof ExecutableRun ? ((ExecutableRun)run).getProject().getName() : ".SYSTEM_TASK");
        String workspaceName = (run instanceof ExecutableRun ? ((ExecutableRun)run).getWorkspace().getName() : ".SYSTEM_TASK");
        String executableName = (run instanceof ExecutableRun ? ((ExecutableRun)run).getExecutable().getName() : ".SYSTEM_TASK");

        run.specializedPreProcess(this);

        JsonArray arguments = new JsonArray();
        for (Object object : run.getArguments()) {
          arguments.add(object.toString());
        }
        WrappedJson environments = new WrappedJson();
        environments.merge(run.getActualComputer().getEnvironments());
        environments.merge(run.getEnvironments());

        Path remoteBinPath = run.getRemoteBinPath();
        TaskJson taskJson = new TaskJson(projectName, workspaceName, executableName,
          remoteBinPath == null ? null : remoteBinPath.toString(),
          run.getCommand(), arguments, environments.toJsonObject()
        );
        //putText(job, TASK_JSON, taskJson.toString());
        envelope.add(new PutTextFileMessage(run.getLocalPath().resolve(TASK_JSON), taskJson.toString()));

        String jvmActivationCommand = getJvmActivationCommand().replaceAll("\"", "\\\"");
        if (!jvmActivationCommand.trim().equals("")) {
          jvmActivationCommand += ";";
        }
        //putText(job, BATCH_FILE, "java -jar '" + getWaffleServantPath(this, computer) + "' '" + parseHomePath(computer.getWorkBaseDirectory()) + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'");
        envelope.add(new PutTextFileMessage(run.getLocalPath().resolve(BATCH_FILE),
          "sh -c \"" + jvmActivationCommand + "java -jar '" + getWaffleServantPath()
          + "' '" + getWorkBaseDirectory() + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'\""));
        //+ "' '" + parseHomePath(computer.getWorkBaseDirectory()) + "' exec '" + getRunDirectory(job.getRun()).resolve(TASK_JSON) + "'"));

        Path remoteExecutableBaseDirectory = getExecutableBaseDirectory(job);
        if (remoteExecutableBaseDirectory != null && !exists(remoteExecutableBaseDirectory.toAbsolutePath())) {
          envelope.add(run.getBinPath());
          envelope.add(new ChangePermissionMessage(remoteExecutableBaseDirectory.resolve(Executable.BASE), "a-w"));
        }

        envelope.add(run.getBasePath());

        job.setState(State.Prepared);
        InfoLogMessage.issue(job.getRun(), "was prepared");
      }
    }
  }

  public String getJvmActivationCommand() {
    return computer.getJvmActivationCommand();
  }

  public Path getWorkBaseDirectory() throws FailedToControlRemoteException {
    return parseHomePath(computer.getWorkBaseDirectory());
  }

  public Path getBaseDirectory(ComputerTask run) throws FailedToControlRemoteException {
    return getRunDirectory(run).resolve(Executable.BASE);
  }

  public Path getRunDirectory(ComputerTask run) throws FailedToControlRemoteException {
    //Computer computer = run.getActualComputer();
    //Path path = parseHomePath(computer.getWorkBaseDirectory()).resolve(run.getLocalDirectoryPath());
    Path path = parseHomePath(getWorkBaseDirectory().toString()).resolve(run.getLocalPath());
    createDirectories(path);
    return path;
  }

  Path getExecutableBaseDirectory(AbstractTask job) throws FailedToControlRemoteException, RunNotFoundException {
    Path remoteBinPath = job.getRun().getRemoteBinPath();
    if (remoteBinPath == null) {
      return null;
    } else {
      return parseHomePath(job.getComputer().getWorkBaseDirectory()).resolve(remoteBinPath);
    }
  }

  void jobFinalizing(AbstractTask job) throws WaffleException {
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
    return submitter.parseHomePath(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
    //return Paths.get(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
  }

  public Path getWaffleServantPath() throws FailedToControlRemoteException {
    //return submitter.parseHomePath(computer.getWorkBaseDirectory()).resolve(ServantJarFile.JAR_FILE);
    return getWaffleServantPath(this, computer);
  }

  public static boolean checkWaffleServant(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(Inspector.Mode.Normal, computer);
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

  public static WrappedJson getXsubTemplate(Computer computer, boolean retry) throws RuntimeException, WaffleException {
    AbstractSubmitter submitter = getInstance(Inspector.Mode.Normal, computer);
    WrappedJson jsonObject = new WrappedJson();
    if (!(submitter instanceof AbstractSubmitterWrapper)) {
      submitter.connect(retry);
      Envelope request = new Envelope(Constants.WORK_DIR);
      request.add(new SendXsubTemplateMessage());
      try {
        Envelope response = submitter.sendAndReceiveEnvelope(request);
        for (XsubTemplateMessage message : response.getMessageBundle().getCastedMessageList(XsubTemplateMessage.class)) {
          jsonObject = new WrappedJson(message.getTemplate());
          break;
        }
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
      submitter.close();
    }
    return jsonObject;
  }

  public static WrappedJson getParameters(Computer computer) {
    AbstractSubmitter submitter = getInstance(Inspector.Mode.Normal, computer);
    WrappedJson jsonObject = submitter.getDefaultParameters(computer);
    return jsonObject;
  }

  protected final boolean isSubmittable(Computer computer, ComputerTask next) {
    return isSubmittable(computer, next, getJobList(Inspector.Mode.Normal, computer), getJobList(Inspector.Mode.System, computer));
  }

  protected final boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<AbstractTask>... lists) {
    ArrayList<ComputerTask> combinedList = new ArrayList<>();
    for (ArrayList<AbstractTask> list : lists) {
      for (AbstractTask job : list) {
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

  protected static ArrayList<AbstractTask> getJobList(Inspector.Mode mode, Computer computer) {
    if (mode.equals(Inspector.Mode.Normal)) {
      return ExecutableRunTask.getList(computer);
    } else {
      return SystemTask.getList(computer);
    }
  }

  protected static AbstractTask findJobFromStore(byte type, String id) {
    WaffleId targetId = WaffleId.valueOf(id);
    if (type == SystemTaskStore.TYPE_CODE) {
      for (SystemTask job : SystemTask.getList()) {
        if (job.getId().equals(targetId)) {
          return job;
        }
      }
    } else {
      for (ExecutableRunTask job : ExecutableRunTask.getList()) {
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

        AbstractTask job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setState(State.Excepted);
        }
      }

      for (PutFileMessage message : response.getMessageBundle().getCastedMessageList(PutFileMessage.class)) {
        message.putFile();
      }

      for (UpdateResultMessage message : response.getMessageBundle().getCastedMessageList(UpdateResultMessage.class)) {
        AbstractTask job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          if (job instanceof ExecutableRunTask) {
            ExecutableRun run = ((ExecutableRunTask) job).getRun();
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
        AbstractTask job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setState(State.Canceled);
        }
      }

      for (UpdateJobIdMessage message : response.getMessageBundle().getCastedMessageList(UpdateJobIdMessage.class)) {
        AbstractTask job = findJobFromStore(message.getType(), message.getId());
        if (job != null) {
          job.setJobId(message.getJobId());
          job.setState(State.Submitted);
          job.getRun().setRemoteWorkingDirectoryLog(message.getWorkingDirectory());
          InfoLogMessage.issue(job.getRun(), "was submitted");
        }
      }

      for (UpdateStatusMessage message : response.getMessageBundle().getCastedMessageList(UpdateStatusMessage.class)) {
        AbstractTask job = findJobFromStore(message.getType(), message.getId());
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

      Envelope replies = new Envelope(Constants.WORK_DIR);
      for (SendValueMessage message : response.getMessageBundle().getCastedMessageList(SendValueMessage.class)) {
        String value = ExecutableRun.getResultFromFullKey(message.getKey());
        if (message.getFilterOperator().equals("")
          || (new Filter(message.getFilterOperator(), message.getFilterValue())).apply(value)
        ) {
          replies.add(new PutValueMessage(message, value == null ? "null" : value));
        }
      }
      if (!replies.isEmpty()) {
        sendAndReceiveEnvelope(replies);
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

    ArrayList<AbstractTask> submittedJobList = new ArrayList<>();
    ArrayList<AbstractTask> runningJobList = new ArrayList<>();
    ArrayList<AbstractTask> cancelJobList = new ArrayList<>();

    for (AbstractTask job : new ArrayList<>(getJobList(mode, computer))) {
      if (Main.hibernatingFlag) { return; }

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

    if (!Main.hibernatingFlag) {
      processSubmitted(envelope, submittedJobList, runningJobList, cancelJobList);
    }

    if (!Main.hibernatingFlag) {
      preparingProcessorManager.startup();
    }

    isRunning = false;

    processRequestAndResponse(envelope);

    return;
  }

  public void processSubmitted(Envelope envelope, ArrayList<AbstractTask> submittedJobList, ArrayList<AbstractTask> runningJobList, ArrayList<AbstractTask> cancelJobList) throws FailedToControlRemoteException {
    submittedJobList.addAll(runningJobList);

    for (AbstractTask job : cancelJobList) {
      if (Main.hibernatingFlag) { return; }
      try {
        cancel(envelope, job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    for (AbstractTask job : submittedJobList) {
      if (Main.hibernatingFlag) { return; }
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

      ArrayList<AbstractTask> submittedJobList = new ArrayList<>();
      ArrayList<AbstractTask> createdJobList = new ArrayList<>();
      ArrayList<AbstractTask> preparedJobList = new ArrayList<>();

      for (AbstractTask job : new ArrayList<>(getJobList(mode, computer))) {
        if (Main.hibernatingFlag) { return; }
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

      if (!Main.hibernatingFlag) {
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

  public void processPreparing(Envelope envelope, ArrayList<AbstractTask> submittedJobList, ArrayList<AbstractTask> createdJobList, ArrayList<AbstractTask> preparedJobList) throws FailedToControlRemoteException {
    reducePreperingTask(createdJobList, submittedJobList, preparedJobList);

    ArrayList<AbstractTask> queuedJobList = new ArrayList<>();
    queuedJobList.addAll(preparedJobList);
    queuedJobList.addAll(createdJobList);

    createdJobList.stream().parallel().forEach((job)->{
      try {
        prepareJob(envelope, job);
      } catch (Exception e) {
        ErrorLogMessage.issue(e);
      }
    });

    for (AbstractTask job : queuedJobList) {
      if (Main.hibernatingFlag) { return; }

      try {
        ComputerTask run = job.getRun();;
        if (isSubmittable(computer, run, submittedJobList)) {

          if (run instanceof ExecutableRun) {
            ExecutableRun executableRun = (ExecutableRun) run;
            if (executableRun.getExecutable().isParallelProhibited()
              && !executableRun.getWorkspace().acquireExecutableLock(executableRun)) {
              continue;
            }
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


    /*
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
     */
  }

  private void reducePreperingTask(ArrayList<AbstractTask> createdJobList, ArrayList<AbstractTask> submittedJobList, ArrayList<AbstractTask> preparedJobList) {
    if (preparingSize < submittedJobList.size() * 2) {
      preparingSize = submittedJobList.size() * 2;
    }
    int space = preparingSize - preparedJobList.size();
    space = Math.max(space, 0);

    int currentSize = createdJobList.size();
    for (int i = createdJobList.size() -1; i >= space; i -= 1) {
      createdJobList.remove(i);
    }
  }

  class FinishedProcessor extends Thread {
    @Override
    public void run() {
      ArrayList<AbstractTask> finalizingJobList = new ArrayList<>();

      do {
        if (Main.hibernatingFlag) { return; }

        Envelope envelope = new Envelope(Constants.WORK_DIR);

        finalizingJobList.clear();

        for (AbstractTask job : new ArrayList<>(getJobList(mode, computer))) {
          if (Main.hibernatingFlag) { return; }

          try {
            ComputerTask run = job.getRun();

            if (!job.exists() && run.isRunning()) {
              job.cancel();
              WarnLogMessage.issue(run, "The task file is not exists; The task will cancel.");
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

  public void processFinished(ArrayList<AbstractTask> finalizingJobList) throws FailedToControlRemoteException {
    for (AbstractTask job : finalizingJobList) {
      if (Main.hibernatingFlag) { return; }

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
