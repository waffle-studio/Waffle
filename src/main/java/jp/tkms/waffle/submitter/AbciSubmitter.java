package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.extractor.AbstractParameterExtractor;
import jp.tkms.waffle.extractor.RubyParameterExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

public class AbciSubmitter extends SshSubmitter {
  protected static final String PACK_BATCH_FILE = "pack_batch.sh";

  UUID currentPackId = null;
  HashMap<UUID, ArrayList<Job>> queuedJobList = new HashMap<>();
  HashMap<UUID, String> packBatchTextList = new HashMap<>();
  HashMap<Job, UUID> jobPackMap = new HashMap<>();
  int remainingJobSize = 0;

  HashMap<UUID, PackWaitThread> packWaitThreadMap = new HashMap<>();
  Semaphore packWaitThreadSemaphore = new Semaphore(1);
  class PackWaitThread extends Thread {
    private int waitTime = 0;
    private ArrayList<Job> readyJobList = new ArrayList<>();
    private UUID packId;

    public PackWaitThread(UUID packId) {
      super();
      this.packId = packId;
    }

    void resetWaitTime() {
      waitTime = 5;
    }

    void addReadyJob(Job job) {
      readyJobList.add(job);
    }

    @Override
    public void run() {
      resetWaitTime();

      while (readyJobList.size() < queuedJobList.get(packId).size() || waitTime >= 0) {
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

      submitQueue(packId);

      packWaitThreadSemaphore.release();
    }
  };

  public AbciSubmitter(Host host) {
    super(host);
  }

  synchronized private void submitQueue(UUID packId) {
    putText(packId, PACK_BATCH_FILE, packBatchTextList.get(packId) + "EOF\n");
    host.setParameter("resource_type_num", AbciResourceSelector.getResourceText(queuedJobList.get(packId).size()));
    String resultJson = exec(xsubSubmitCommand(packId));
    for (Job job : queuedJobList.get(packId)) {
      processXsubSubmit(job, resultJson);
    }
    queuedJobList.remove(packId);
    packBatchTextList.remove(packId);
    packWaitThreadMap.remove(packId);
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

    if (currentPackId == null || !queuedJobList.containsKey(currentPackId)
      || queuedJobList.get(currentPackId).size() >= AbciResourceSelector.getPackSize(remainingJobSize)) {
      UUID packId = UUID.randomUUID();
      currentPackId = packId;
      queuedJobList.put(packId, new ArrayList<>());
      packBatchTextList.put(packId,
        "#!/bin/sh\n\n" +
          "run() {\n" +
          "cd $1\n" +
          "sh batch.sh\n" +
          "}\n" +
          "export -f run\n" +
          "xargs -n 1 -P 65535 -I{} sh -c 'run {}' << EOF\n"
        );

      PackWaitThread thread = new PackWaitThread(packId);
      packWaitThreadMap.put(packId, thread);
      thread.start();
    }

    UUID packId = currentPackId;
    jobPackMap.put(job, packId);
    queuedJobList.get(packId).add(job);
    PackWaitThread packWaitThread = packWaitThreadMap.get(packId);
    remainingJobSize -= 1;

    packWaitThread.resetWaitTime();
    packWaitThreadSemaphore.release();

    try {
      Thread.sleep((int)(2000.0 * Math.random()));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Run run = job.getRun();

    putText(run, BATCH_FILE, makeBatchFileText(run));

    for (ParameterExtractor extractor : ParameterExtractor.getList(run.getSimulator())) {
      AbstractParameterExtractor instance
        = AbstractParameterExtractor.getInstance(RubyParameterExtractor.class.getCanonicalName());
      instance.extract(run, extractor, this);
    }

    putText(run, EXIT_STATUS_FILE, "-2");
    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    prepareSubmission(run);

    job.getRun().setState(Run.State.Queued);

    try {
      packWaitThreadSemaphore.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    packBatchTextList.put(packId, packBatchTextList.get(packId) + getWorkDirectory(run) + "\n");

    packWaitThread.addReadyJob(job);
    packWaitThreadSemaphore.release();
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
        case Finished:
        case Failed:
          job.remove();
      }

      if (Main.hibernateFlag) {
        break;
      }
    }

    remainingJobSize = createdJobList.size();
    ExecutorService executor = Executors.newFixedThreadPool(15);
    ArrayList<Callable<Boolean>> jobThreadList = new ArrayList<>();
    for (Job job : createdJobList) {
      if (submittedCount < maximumNumberOfJobs) {
        jobThreadList.add(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            submit(job);
            return true;
          }
        });
        submittedCount++;
      }
    }

    try {
      executor.invokeAll(jobThreadList);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    executor.shutdown();

    for (PackWaitThread thread : new ArrayList<>(packWaitThreadMap.values())) {
      if (thread != null) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void hibernate() {
    super.hibernate();

    for (PackWaitThread thread : packWaitThreadMap.values()) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static class AbciResourceSelector {
    public static String getResourceText(int size) {
      if (size <= 5) {
        return "rt_C.small=1";
      } else if (size <= 20) {
        return "rt_C.large=1";
      } else {
        return "rt_F=1";
      }
    }

    public static int getPackSize(int jobSize) {
      if (jobSize >= 40) {
        return 40;
      } else if (jobSize >= 15) {
        return 20;
      } else {
        return 3;
      }
    }
  }
}
