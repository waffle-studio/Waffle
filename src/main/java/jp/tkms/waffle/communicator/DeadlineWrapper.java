package jp.tkms.waffle.communicator;

import jp.tkms.waffle.communicator.annotation.CommunicatorDescription;
import jp.tkms.waffle.communicator.process.RemoteProcess;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.inspector.Inspector;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.inspector.InspectorMaster;
import jp.tkms.waffle.sub.servant.Envelope;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@CommunicatorDescription("Deadline Wrapper (setting computer expiration datetime)")
public class DeadlineWrapper extends AbstractSubmitterWrapper {
  public static final String KEY_TARGET_COMPUTER = "target_computer";
  public static final String KEY_DEADLINE = "deadline";
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");

  public DeadlineWrapper(Computer computer) {
    super(computer);
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
  public Path parseHomePath(String pathString) {
    return null;
  }

  @Override
  public Path getAbsolutePath(Path path) throws FailedToControlRemoteException {
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
  protected RemoteProcess startProcess(String command) throws FailedToControlRemoteException {
    return null;
  }

  @Override
  public void chmod(int mod, Path path) throws FailedToControlRemoteException {

  }

  @Override
  public String getFileContents(ComputerTask run, Path path) throws FailedToTransferFileException {
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {

  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath, Boolean isDir) throws FailedToTransferFileException {

  }

  @Override
  protected boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<ComputerTask> list) {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_DEADLINE, ""));
      if (new Date().before(deadline)) {
        Computer targetComputer = Computer.getInstance(computer.getParameters().getString(KEY_TARGET_COMPUTER, ""));
        if (targetComputer != null) {
          AbstractSubmitter targetSubmitter = AbstractSubmitter.getInstance((next instanceof SystemTask ? Inspector.Mode.System : Inspector.Mode.Normal), targetComputer);
          return targetSubmitter.isSubmittable(targetComputer, next, list);
        }
      }
    } catch (ParseException e) {
      WarnLogMessage.issue(e);
    }

    return false;
  }

  @Override
  public boolean processPreparing(Envelope envelope, ArrayList<AbstractTask> submittedJobList, ArrayList<AbstractTask> createdJobList, ArrayList<AbstractTask> preparedJobList) throws FailedToControlRemoteException {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_DEADLINE, ""));
      if (new Date().before(deadline)) {
        for (AbstractTask job : createdJobList) {
          Computer targetComputer = Computer.getInstance(computer.getParameters().getString(KEY_TARGET_COMPUTER, ""));
          if (targetComputer != null) {
            AbstractSubmitter targetSubmitter = AbstractSubmitter.getInstance(Inspector.Mode.Normal, targetComputer);
            try {
              if (targetSubmitter.isSubmittable(targetComputer, job.getRun())) {
                job.replaceComputer(targetComputer);
              }
            } catch (RunNotFoundException e) {
              WarnLogMessage.issue(e);
              job.remove();
            }
          }
        }
      }
    } catch (ParseException e) {
      WarnLogMessage.issue(e);
    }

    InspectorMaster.forceCheck();

    return true;
  }

  @Override
  public WrappedJson getDefaultParameters(Computer computer) {
    WrappedJson jsonObject = new WrappedJson();
    jsonObject.put(KEY_TARGET_COMPUTER, "LOCAL");
    jsonObject.put(KEY_DEADLINE, "2050-01-01 00:00");
    return jsonObject;
  }
}
