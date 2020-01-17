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
    private int waitTime = 0;
    void resetWaitTime() {
      waitTime = 5;
    }
    void skipWaitTime() {
      waitTime = 0;
    }

    @Override
    public void run() {
      resetWaitTime();

      while (!isInterrupted() && waitTime >= 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        try {
          packWaitThreadSemaphore.acquire();
        } catch (InterruptedException e) {
        }
        waitTime -= 1;
        packWaitThreadSemaphore.release();
      }

      try {
        packWaitThreadSemaphore.acquire();
      } catch (InterruptedException e) {
      }

      submitQueue();

      packWaitThreadSemaphore.release();
    }
  };

  public AbciSubmitter(Host host) {
    super(host);
  }

  private void submitQueue() {
    packBatchText += "EOF\n";
    putText(packId, PACK_BATCH_FILE, packBatchText);
    String resultJson = exec(xsubSubmitCommand(packId));
    for (Job job : queuedJobList) {
      processXsubSubmit(job, resultJson);
    }
    packId = null;
    queuedJobList.clear();
  }

  String xsubSubmitCommand(UUID packId) {
    return xsubCommand(packId) + " " + PACK_BATCH_FILE;
  }

  @Override
  public void submit(Job job) {
    try {
      packWaitThreadSemaphore.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (packId == null) {
      packId = UUID.randomUUID();
      queuedJobList.clear();
      packBatchText = "#!/bin/sh\n\n" +
        "run() {\n" +
        "cd $1\n" +
        "sh batch.sh\n" +
        "}\n" +
        "export -f run\n" +
        "xargs -n 1 -P 65535 -I{} sh -c 'run {}' << EOF\n";
      packWaitThread = new PackWaitThread();
      packWaitThread.start();
    }

    jobPackMap.put(job, packId);

    Run run = job.getRun();

    putText(run, BATCH_FILE, makeBatchFileText(run));

    for (ParameterExtractor extractor : ParameterExtractor.getList(run.getSimulator())) {
      AbstractParameterExtractor instance = AbstractParameterExtractor.getInstance(RubyParameterExtractor.class.getCanonicalName());
      instance.extract(run, extractor, this);
    }

    putText(run, EXIT_STATUS_FILE, "-2");
    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    packBatchText += getWorkDirectory(run) + "\n";

    prepareSubmission(run);

    queuedJobList.add(job);
    job.getRun().setState(Run.State.Queued);
    packWaitThread.resetWaitTime();
    packWaitThreadSemaphore.release();

    if (queuedJobList.size() >= 10) {
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

    ArrayList<Job> createdJobList = new ArrayList<>();
    int submittedCount = 0;

    updatedPackJson.clear();

    for (Job job : jobList) {
      Run run = job.getRun();
      switch (run.getState()) {
        case Created:
          if (createdJobList.size() <= maximumNumberOfJobs) {
            createdJobList.add(job);
          }
          break;
        case Queued:
          submittedCount++;
          job.remove();
          break;
        case Submitted:
        case Running:
          Run.State state = update(job);
          if (!(Run.State.Finished.equals(state) || Run.State.Failed.equals(state))) {
            submittedCount++;
          }
          break;
      }
    }

    ArrayList<Thread> threadList = new ArrayList<>();
    for (Job job : createdJobList) {
      if (submittedCount < maximumNumberOfJobs) {
        Thread thread = new Thread(() -> submit(job));
        thread.start();
        threadList.add(thread);
        submittedCount++;
      }
    }

    for (Thread thread : threadList) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void hibernate() {
    super.hibernate();
    if ( packWaitThread != null ) {
      packWaitThread.skipWaitTime();
      try {
        packWaitThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static class AbciResourceSelector {
    public static String getResourceText(int size) {
      if (size <= 10) {
        return "rt_C.small=1";
      } else if (size <= 40) {
        return "rt_C.large=1";
      } else {
        return "rt_F=1";
      }
    }

    public static int getPackSize(int jobSize) {
      if (jobSize > 50) {
        return 80;
      } else if (jobSize > 20) {
        return 40;
      } else {
        return 10;
      }
    }
  }
}
