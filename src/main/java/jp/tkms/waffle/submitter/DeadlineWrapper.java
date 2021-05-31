package jp.tkms.waffle.submitter;

import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.job.ExecutableRunJob;
import jp.tkms.waffle.data.job.SystemTaskJob;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.ComputerState;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DeadlineWrapper extends AbstractSubmitter {
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
  public void processCreated(ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
  }

  @Override
  protected boolean isSubmittable(Computer computer, AbstractJob next, ArrayList<AbstractJob> list) {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_DEADLINE));
      if (new Date().before(deadline)) {
        Computer targetComputer = Computer.getInstance(computer.getParameters().getString(KEY_TARGET_COMPUTER));
        if (targetComputer != null) {
          AbstractSubmitter targetSubmitter = AbstractSubmitter.getInstance((next instanceof SystemTaskJob ? PollingThread.Mode.System : PollingThread.Mode.Normal), targetComputer);
          return targetSubmitter.isSubmittable(targetComputer, next, list);
        }
      }
    } catch (ParseException e) {
      WarnLogMessage.issue(e);
    }

    return false;
  }

  @Override
  public void processPrepared(ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    try {
      Date deadline = dateFormat.parse(computer.getParameters().getString(KEY_DEADLINE));
      if (new Date().before(deadline)) {
        for (AbstractJob job : createdJobList) {
          Computer targetComputer = Computer.getInstance(computer.getParameters().getString(KEY_TARGET_COMPUTER));
          if (targetComputer != null) {
            AbstractSubmitter targetSubmitter = AbstractSubmitter.getInstance(PollingThread.Mode.Normal, targetComputer);
            if (targetSubmitter.isSubmittable(targetComputer, job)) {
              try {
                job.replaceComputer(targetComputer);
              } catch (RunNotFoundException e) {
                WarnLogMessage.issue(e);
                job.remove();
              }
            }
          }
        }
      }
    } catch (ParseException e) {
      WarnLogMessage.issue(e);
    }

    PollingThread.startup();
  }

  @Override
  public JSONObject getDefaultParameters(Computer computer) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_TARGET_COMPUTER, "LOCAL");
    jsonObject.put(KEY_DEADLINE, "2050-01-01 00:00");
    return jsonObject;
  }
}
