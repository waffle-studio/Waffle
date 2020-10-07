package jp.tkms.waffle.submitter;

import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.*;

public class RoundRobinSubmitter extends AbstractSubmitter {
  public static final String KEY_TARGET_HOSTS = "target_hosts";



  public RoundRobinSubmitter(Host host) {
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
  public String getFileContents(SimulatorRun run, Path path) throws FailedToTransferFileException {
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {

  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {

  }

  @Override
  public double getMaximumNumberOfThreads(Host host) {
    double num = 0.0;

    JSONArray targetHosts = host.getParameters().getJSONArray(KEY_TARGET_HOSTS);
    if (targetHosts != null) {
      for (Object object : targetHosts.toList()) {
        Host targetHost = Host.getInstance(object.toString());
        if (targetHost != null && targetHost.getState().equals(HostState.Viable)) {
          num += targetHost.getMaximumNumberOfThreads();
        }
      }
    }

    return (num < host.getMaximumNumberOfThreads() ? num : host.getMaximumNumberOfThreads());
  }

  @Override
  public double getAllocableMemorySize(Host host) {
    double size = 0.0;

    JSONArray targetHosts = host.getParameters().getJSONArray(KEY_TARGET_HOSTS);
    if (targetHosts != null) {
      for (Object object : targetHosts.toList()) {
        Host targetHost = Host.getInstance(object.toString());
        if (targetHost != null && targetHost.getState().equals(HostState.Viable)) {
          size += targetHost.getAllocableMemorySize();
        }
      }
    }

    return (size < host.getAllocableMemorySize() ? size : host.getAllocableMemorySize());
  }

  @Override
  public void processJobLists(Host host, ArrayList<Job> createdJobList, ArrayList<Job> preparedJobList, ArrayList<Job> submittedJobList, ArrayList<Job> runningJobList, ArrayList<Job> cancelJobList) throws FailedToControlRemoteException {
    for (Job job : cancelJobList) {
      try {
        cancel(job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    double globalFreeThread = host.getMaximumNumberOfThreads();
    double globalFreeMemory = host.getAllocableMemorySize();

    LinkedList<Host> passableHostList = new LinkedList<>();
    JSONArray targetHosts = host.getParameters().getJSONArray(KEY_TARGET_HOSTS);
    if (targetHosts != null) {
      for (Object object : targetHosts.toList()) {
        Host targetHost = Host.getInstance(object.toString());
        if (targetHost != null && targetHost.getState().equals(HostState.Viable)) {
          passableHostList.add(targetHost);

          for (Job job : Job.getList(targetHost)) {
            try {
              SimulatorRun run = job.getRun();
              if (run.getHost().equals(host)) {
                globalFreeThread -= run.getSimulator().getRequiredThread();
                globalFreeMemory -= run.getSimulator().getRequiredMemory();
              }
            } catch (RunNotFoundException e) {
            }
          }
        }
      }
    }

    int targetHostCursor = 0;
    if (globalFreeThread > 0.0 && globalFreeMemory > 0.0) {
      if (passableHostList.size() > 0) {
        for (Job job : createdJobList) {
          Host targetHost = passableHostList.get(targetHostCursor);

          if (true /* isSubmittable(targetHost, job) */) {
            try {
              job.replaceHost(targetHost);
            } catch (RunNotFoundException e) {
              WarnLogMessage.issue(e);
              job.remove();
            }
          }

          targetHostCursor += 1;
          if (targetHostCursor >= passableHostList.size()) {
            targetHostCursor = 0;
          }
        }
      }
    }

    PollingThread.startup();
  }

  @Override
  public JSONObject getDefaultParameters(Host host) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("target_hosts", new JSONArray());
    return jsonObject;
  }
}
