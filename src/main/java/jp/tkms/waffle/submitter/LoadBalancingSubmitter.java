package jp.tkms.waffle.submitter;

import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
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

public class LoadBalancingSubmitter extends MultiComputerSubmitter {

  public LoadBalancingSubmitter(Computer computer) {
    super(computer);
  }

  /*
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
   */

  @Override
  public void processPrepared(ArrayList<AbstractJob> submittedJobList, ArrayList<AbstractJob> createdJobList, ArrayList<AbstractJob> preparedJobList) throws FailedToControlRemoteException {
    double globalFreeThread = computer.getMaximumNumberOfThreads();
    double globalFreeMemory = computer.getAllocableMemorySize();
    int globalFreeJobSlot = computer.getMaximumNumberOfJobs();

    ArrayList<Computer> passableComputerList = new ArrayList<>();
    JSONArray targetComputers = computer.getParameters().getJSONArray(KEY_TARGET_COMPUTERS);
    if (targetComputers != null) {
      for (Object object : targetComputers.toList()) {
        Computer targetComputer = Computer.getInstance(object.toString());
        if (targetComputer != null && targetComputer.getState().equals(ComputerState.Viable)) {
          passableComputerList.add(targetComputer);

          for (AbstractJob job : getJobList(PollingThread.Mode.Normal, targetComputer)) {
            try {
              ComputerTask run = job.getRun();
              if (run.getComputer().equals(computer)) {
                globalFreeThread -= run.getRequiredThread();
                globalFreeMemory -= run.getRequiredMemory();
                globalFreeJobSlot -= 1;
              }
            } catch (RunNotFoundException e) {
            }
          }
        }
      }
    }

    int targetHostCursor = 0;
    if (globalFreeThread > 0.0 && globalFreeMemory > 0.0 && globalFreeJobSlot > 0 && passableComputerList.size() > 0) {
      if (passableComputerList.size() > 0) {
        for (AbstractJob job : createdJobList) {
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
}
