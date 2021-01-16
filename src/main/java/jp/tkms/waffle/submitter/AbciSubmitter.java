package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.exception.WaffleException;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.State;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class AbciSubmitter extends SshSubmitter {
  protected static final String PACK_BATCH_FILE = "pack_batch.sh";

  HashMap<String, String> updatedPackJson = new HashMap<>();
  int holdingJob = 0;

  public AbciSubmitter(Computer computer) {
    super(computer);
  }

  public void putText(UUID packId, String path, String text) throws FailedToTransferFileException {
    try {
      session.putText(text, path, getWorkDirectory(packId));
    } catch (JSchException | FailedToControlRemoteException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  String getWorkDirectory(UUID packId) throws FailedToControlRemoteException {
    Path path = parseHomePath(computer.getWorkBaseDirectory()).resolve(RUN_DIR).resolve("pack").resolve(packId.toString());
    createDirectories(path);
    return path.toString();
  }

  String xsubCommand(UUID packId) throws FailedToControlRemoteException {
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(computer) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getWorkDirectory(packId) + "'; " +
      "XSUB_TYPE=$XSUB_TYPE $XSUB_COMMAND -p '" + computer.getXsubParameters().toString().replaceAll("'", "\\\\'") + "' ";
  }

  public String getResourceText(int size) {
    if (size <= 5) {
      return "rt_C.small=1";
    } else if (size <= 20) {
      return "rt_C.large=1";
    } else {
      return "rt_F=1";
    }
  }

  @Override
  public State update(Job job) throws RunNotFoundException {
    ExecutableRun run = job.getRun();
    if (!updatedPackJson.containsKey(job.getJobId())) {
      updatedPackJson.put(job.getJobId(), exec(xstatCommand(job)));
    }
    processXstat(job, updatedPackJson.get(job.getJobId()));
    return run.getState();
  }

  @Override
  public double getMaximumNumberOfThreads(Computer computer) {
    return 40.0;
  }

  @Override
  public void processJobLists(Computer computer, ArrayList<Job> createdJobList, ArrayList<Job> preparedJobList, ArrayList<Job> submittedJobList, ArrayList<Job> runningJobList, ArrayList<Job> cancelJobList) throws FailedToControlRemoteException {
    updatedPackJson.clear();

    HashSet<String> cancelJobIdSet = new HashSet<>();
    for (Job job : cancelJobList) {
      cancelJobIdSet.add(job.getJobId());
      try {
        job.setState(State.Canceled);
      } catch (RunNotFoundException e) { }
    }

    submittedJobList.addAll(runningJobList);
    HashSet<String> jobIdSet = new HashSet<>();
    for (Job job : submittedJobList) {
      if (Main.hibernateFlag) { break; }

      try {
        switch (update(job)) {
          case Finished:
          case Failed:
          case Excepted:
          case Canceled:
            break;
          default:
            jobIdSet.add(job.getJobId());
        }
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }

    for (Job job : cancelJobList) {
      String jobId = job.getJobId();
      if (cancelJobIdSet.contains(jobId) && jobIdSet.contains(jobId)) {
        try {
          cancel(job);
        } catch (RunNotFoundException e) { }
        cancelJobIdSet.remove(jobId);
      }
    }

    ArrayList<Job> queuedJobList = new ArrayList<>();
    queuedJobList.addAll(preparedJobList);
    for (Job job : createdJobList) {
      if (Main.hibernateFlag) { break; }
      try {
        if (queuedJobList.size() < getMaximumNumberOfThreads(computer)) {
          prepareJob(job);
          queuedJobList.add(job);
        } else {
          break;
        }
      } catch (WaffleException e) {
        WarnLogMessage.issue(e);
        try {
          job.setState(State.Excepted);
        } catch (RunNotFoundException ex) { }
        throw new FailedToControlRemoteException(e);
      }
    }

    if (Main.hibernateFlag) {
      return;
    }

    if (jobIdSet.size() < computer.getMaximumNumberOfThreads() && queuedJobList.size() > 0) {
      if (queuedJobList.size() >= getMaximumNumberOfThreads(computer) || holdingJob == queuedJobList.size()) {
        UUID packId = UUID.randomUUID();
        StringBuilder packBatchTextBuilder = new StringBuilder();
        packBatchTextBuilder.append(
          "#!/bin/sh\n\n" +
            "run() {\n" +
            "cd $1\n" +
            "sh batch.sh\n" +
            "}\n" +
            "export -f run\n" +
            "xargs -n 1 -P 65535 -I{} sh -c 'run {}' << EOF\n");

        for (Job job : queuedJobList) {
          try {
            packBatchTextBuilder.append(getRunDirectory(job.getRun()) + "\n");
          } catch (RunNotFoundException e) {
            WarnLogMessage.issue(e);
            try {
              job.setState(State.Excepted);
            } catch (RunNotFoundException ex) { }
          }
        }

        packBatchTextBuilder.append("EOF\n");

        try {
          putText(packId, PACK_BATCH_FILE, packBatchTextBuilder.toString());
          computer.setParameter("resource_type_num", getResourceText(queuedJobList.size()));
          String resultJson = exec(xsubCommand(packId) + " " + PACK_BATCH_FILE);
          for (Job job : queuedJobList) {
            try {
              processXsubSubmit(job, resultJson);
            } catch (Exception e) {
              WarnLogMessage.issue(e);
              job.setState(State.Excepted);
            }
          }
        } catch (FailedToTransferFileException | FailedToControlRemoteException | RunNotFoundException e) {
          ErrorLogMessage.issue(e);
        }

        holdingJob = 0;
        skipPolling();
      } else {
        if (holdingJob != queuedJobList.size()) {
          skipPolling();
        }
        holdingJob = queuedJobList.size();
      }
    }
  }
}
