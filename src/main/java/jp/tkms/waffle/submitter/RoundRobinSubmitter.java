package jp.tkms.waffle.submitter;

import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.ComputerState;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.*;

public class RoundRobinSubmitter extends AbstractSubmitter {
  public static final String KEY_TARGET_COMPUTERS = "target_computers";



  public RoundRobinSubmitter(Computer computer) {
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
  public void close() {
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
  public void putText(Job job, Path path, String text) throws FailedToTransferFileException, RunNotFoundException {

  }

  @Override
  public String getFileContents(ExecutableRun run, Path path) throws FailedToTransferFileException {
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {

  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {

  }

  @Override
  public double getMaximumNumberOfThreads(Computer computer) {
    double num = 0.0;

    JSONArray targetComputers = computer.getParameters().getJSONArray(KEY_TARGET_COMPUTERS);
    if (targetComputers != null) {
      for (Object object : targetComputers.toList()) {
        Computer targetComputer = Computer.getInstance(object.toString());
        if (targetComputer != null && targetComputer.getState().equals(ComputerState.Viable)) {
          num += targetComputer.getMaximumNumberOfThreads();
        }
      }
    }

    return (num < computer.getMaximumNumberOfThreads() ? num : computer.getMaximumNumberOfThreads());
  }

  @Override
  public double getAllocableMemorySize(Computer computer) {
    double size = 0.0;

    JSONArray targetComputers = computer.getParameters().getJSONArray(KEY_TARGET_COMPUTERS);
    if (targetComputers != null) {
      for (Object object : targetComputers.toList()) {
        Computer targetComputer = Computer.getInstance(object.toString());
        if (targetComputer != null && targetComputer.getState().equals(ComputerState.Viable)) {
          size += targetComputer.getAllocableMemorySize();
        }
      }
    }

    return (size < computer.getAllocableMemorySize() ? size : computer.getAllocableMemorySize());
  }

  @Override
  public void processJobLists(Computer computer, ArrayList<Job> createdJobList, ArrayList<Job> preparedJobList, ArrayList<Job> submittedJobList, ArrayList<Job> runningJobList, ArrayList<Job> cancelJobList) throws FailedToControlRemoteException {
    for (Job job : cancelJobList) {
      try {
        cancel(job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    double globalFreeThread = computer.getMaximumNumberOfThreads();
    double globalFreeMemory = computer.getAllocableMemorySize();

    LinkedList<Computer> passableComputerList = new LinkedList<>();
    JSONArray targetComputers = computer.getParameters().getJSONArray(KEY_TARGET_COMPUTERS);
    if (targetComputers != null) {
      for (Object object : targetComputers.toList()) {
        Computer targetComputer = Computer.getInstance(object.toString());
        if (targetComputer != null && targetComputer.getState().equals(ComputerState.Viable)) {
          passableComputerList.add(targetComputer);

          for (Job job : Job.getList(targetComputer)) {
            try {
              ExecutableRun run = job.getRun();
              if (run.getComputer().equals(computer)) {
                globalFreeThread -= run.getExecutable().getRequiredThread();
                globalFreeMemory -= run.getExecutable().getRequiredMemory();
              }
            } catch (RunNotFoundException e) {
            }
          }
        }
      }
    }

    int targetHostCursor = 0;
    if (globalFreeThread > 0.0 && globalFreeMemory > 0.0) {
      if (passableComputerList.size() > 0) {
        for (Job job : createdJobList) {
          Computer targetComputer = passableComputerList.get(targetHostCursor);

          if (!isSubmittable(targetComputer, job)) {
            int startOfTargetHostCursor = targetHostCursor;
            targetHostCursor += 1;
            if (targetHostCursor >= passableComputerList.size()) {
              targetHostCursor = 0;
            }
            do {
              targetComputer = passableComputerList.get(targetHostCursor);
            } while (targetHostCursor != startOfTargetHostCursor && !isSubmittable(targetComputer, job));
          }

          if (isSubmittable(targetComputer, job)) {
            try {
              job.replaceComputer(targetComputer);
            } catch (RunNotFoundException e) {
              WarnLogMessage.issue(e);
              job.remove();
            }
          }

          targetHostCursor += 1;
          if (targetHostCursor >= passableComputerList.size()) {
            targetHostCursor = 0;
          }
        }
      }
    }

    PollingThread.startup();
  }

  @Override
  public JSONObject getDefaultParameters(Computer computer) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_TARGET_COMPUTERS, new JSONArray());
    return jsonObject;
  }
}
