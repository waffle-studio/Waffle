package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.extractor.AbstractParameterExtractor;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import jp.tkms.waffle.submitter.util.SshChannel;
import jp.tkms.waffle.submitter.util.SshSession;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class AbciSubmitter extends SshSubmitter {
  protected static final String PACK_BATCH_FILE = "pack_batch.sh";

  UUID packId = null;
  ArrayList<Job> queuedJobList = new ArrayList<>();
  String packBatchText = "";
  HashMap<Job, UUID> jobPackMap = new HashMap<>();

  PackWaitThread packWaitThread = null;
  Semaphore packWaitThreadSemaphore = new Semaphore(1);
  class PackWaitThread extends Thread {
    @Override
    public void run() {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) { }

      try {
        packWaitThreadSemaphore.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      submitQueue();
      packId = null;

      packWaitThreadSemaphore.release();
    }
  };

  public AbciSubmitter(Host host) {
    super(host);
  }

  private void submitQueue() {
    putText(packId, PACK_BATCH_FILE, packBatchText);
    String resultJson = exec(xsubSubmitCommand(packId));
    for (Job job : queuedJobList) {
      processXsubSubmit(job, resultJson);
    }
  }

  String xsubSubmitCommand(UUID packId) {
    return xsubCommand(packId) + " " + PACK_BATCH_FILE;
  }

  @Override
  public synchronized void submit(Job job) {
    try {
      packWaitThreadSemaphore.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (packId == null) {
      packId = UUID.randomUUID();
      queuedJobList.clear();
      packBatchText = "#!/bin/sh\n\n";
      packWaitThread = new PackWaitThread();
      packWaitThread.start();
    }

    queuedJobList.add(job);
    jobPackMap.put(job, packId);
    job.getRun().setState(Run.State.Queued);

    Run run = job.getRun();

    putText(run, BATCH_FILE, makeBatchFileText(run));

    for (ParameterExtractor extractor : ParameterExtractor.getList(run.getSimulator())) {
      AbstractParameterExtractor instance = AbstractParameterExtractor.getInstance(RubyParameterExtractor.class.getCanonicalName());
      instance.extract(run, extractor, this);
    }

    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    packBatchText += "cd " + getWorkDirectory(run) + "\n";
    packBatchText += "sh " + BATCH_FILE + "\n";

    prepareSubmission(run);

    packWaitThreadSemaphore.release();

    if (queuedJobList.size() >= 2) {
      packWaitThread.interrupt();
      try {
        packWaitThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    //processXsubSubmit(job, exec(xsubSubmitCommand(job)));
  }

  public void putText(UUID packId, String path, String text) {
    try {
      session.putText(text, path, getWorkDirectory(packId));
    } catch (JSchException e) {
      e.printStackTrace();
    }
  }

  String getWorkDirectory(UUID packId) {
    String pathString = host.getWorkBaseDirectory() + host.getDirectorySeparetor()
      + RUN_DIR + host.getDirectorySeparetor()
      + "pack" + host.getDirectorySeparetor()
      + packId.toString();

    try {
      session.mkdir(pathString, "~/");
    } catch (JSchException e) {
      e.printStackTrace();
    }

    return toAbsoluteHomePath(pathString);
  }

  String xsubCommand(UUID packId) {
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(host) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getWorkDirectory(packId) + "'; " +
      "XSUB_TYPE=$XSUB_TYPE $XSUB_COMMAND -p '" + host.getXsubParameters().toString().replaceAll("'", "\\\\'") + "' ";
  }

  String xstatCommand(Job job) {
    Host host = job.getHost();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + getXsubBinDirectory(host) + "xstat " + job.getJobId();
  }

  String xdelCommand(Job job) {
    Host host = job.getHost();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + getXsubBinDirectory(host) + "xdel " + job.getJobId();
  }

  HashMap<UUID, String> updatedPackJson = new HashMap<>();
  public Run.State update(Job job) {
    Run run = job.getRun();
    UUID packId = jobPackMap.get(job);
    if (!updatedPackJson.containsKey(packId)) {
      updatedPackJson.put(packId, exec(xstatCommand(job)));
    }
    processXstat(job, updatedPackJson.get(packId));
    return run.getState();
  }

  public void pollingTask(Host host) {
    ArrayList<Job> jobList = Job.getList(host);

    int maximumNumberOfJobs = host.getMaximumNumberOfJobs();

    ArrayList<Job> queuedJobList = new ArrayList<>();
    int submittedCount = 0;

    updatedPackJson.clear();

    for (Job job : jobList) {
      Run run = job.getRun();
      switch (run.getState()) {
        case Created:
          if (queuedJobList.size() <= maximumNumberOfJobs) {
            queuedJobList.add(job);
          }
          break;
        case Queued:
          submittedCount++;
          break;
        case Submitted:
        case Running:
        case Finished:
          Run.State state = update(job);
          if (!Run.State.Finished.equals(state)) {
            submittedCount++;
          }
          break;
        case Failed:
          //job.remove();
      }
    }

    for (Job job : queuedJobList) {
      if (submittedCount < maximumNumberOfJobs) {
        submit(job);
        submittedCount++;
      }
    }
  }
}
