package jp.tkms.waffle.communicator;

import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.internal.task.SystemTask;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.inspector.Inspector;
import jp.tkms.waffle.inspector.InspectorMaster;
import jp.tkms.waffle.sub.servant.Envelope;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BarrierWrapper extends AbstractSubmitterWrapper {
  public static final String KEY_TARGET_COMPUTER = "target_computer";
  public static final String KEY_BARRIER = "barrier";
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");

  public BarrierWrapper(Computer computer) {
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
  protected boolean isSubmittable(Computer computer, ComputerTask next, ArrayList<ComputerTask> list) {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_BARRIER, ""));
      if (new Date().after(deadline)) {
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
  public void processPreparing(Envelope envelope, ArrayList<AbstractTask> submittedJobList, ArrayList<AbstractTask> createdJobList, ArrayList<AbstractTask> preparedJobList) throws FailedToControlRemoteException {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_BARRIER, ""));
      if (new Date().after(deadline)) {
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
  }

  @Override
  public WrappedJson getDefaultParameters(Computer computer) {
    WrappedJson jsonObject = new WrappedJson();
    jsonObject.put(KEY_TARGET_COMPUTER, "LOCAL");
    jsonObject.put(KEY_BARRIER, "2050-01-01 00:00");
    return jsonObject;
  }
}
