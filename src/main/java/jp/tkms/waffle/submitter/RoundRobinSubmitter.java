package jp.tkms.waffle.submitter;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.PollingThread;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.exception.WaffleException;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.HostState;
import jp.tkms.waffle.data.util.State;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

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
  String exec(String command) throws FailedToControlRemoteException {
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
  public int getMaximumNumberOfJobs(Host host) {
    int num = 0;

    for (Host h : Host.getList()) {
      num += h.getMaximumNumberOfJobs();
    }

    return num;
  }

  @Override
  public void processJobLists(Host host, ArrayList<Job> createdJobList, ArrayList<Job> preparedJobList, ArrayList<Job> submittedJobList, ArrayList<Job> runningJobList, ArrayList<Job> cancelJobList) throws FailedToControlRemoteException {
    int maximumOverNumberOfJobs = host.getMaximumNumberOfJobs();

    for (Job job : cancelJobList) {
      try {
        cancel(job);
      } catch (RunNotFoundException e) {
        job.remove();
      }
    }

    LinkedList<Host> passableHostList = new LinkedList<>();
    LinkedList<Integer> passableNumberList = new LinkedList<>();
    JSONArray targetHosts = host.getParameters().getJSONArray(KEY_TARGET_HOSTS);
    if (targetHosts != null) {
      for (Object object : targetHosts.toList()) {
        Host targetHost = Host.getInstance(object.toString());
        if (targetHost != null && targetHost.getState().equals(HostState.Viable)) {
          int passabale = targetHost.getMaximumNumberOfJobs() - Job.getList(targetHost).size() + maximumOverNumberOfJobs;
          if (passabale > 0) {
            passableHostList.add(targetHost);
            passableNumberList.add(targetHost.getMaximumNumberOfJobs() - Job.getList(targetHost).size() + maximumOverNumberOfJobs);
          }
        }
      }
    }

    int targetHostCursor = 0;
    if (passableHostList.size() > 0) {
      for (Job job : createdJobList) {
        Host targetHost = passableHostList.get(targetHostCursor);

        try {
          job.replaceHost(targetHost);
        } catch (RunNotFoundException e) {
          WarnLogMessage.issue(e);
          job.remove();
        }

        passableNumberList.set(targetHostCursor, passableNumberList.get(targetHostCursor) -1);
        if (passableNumberList.get(targetHostCursor) <= 0) {
          passableHostList.remove(targetHostCursor);
          passableNumberList.remove(targetHostCursor);
          if (passableHostList.size() <= 0) {
            break;
          }
        }
        targetHostCursor += 1;
        if (targetHostCursor >= passableHostList.size()) {
          targetHostCursor = 0;
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
