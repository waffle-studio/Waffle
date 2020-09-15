package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.log.ErrorLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.State;

import java.nio.file.Path;
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

  private static final Object objectLocker = new Object();

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

        synchronized (objectLocker) {
          waitTime -= 1;
        }
      }

      submitQueue(packId);
    }
  };

  public AbciSubmitter(Host host) {
    super(host);
  }

  private void submitQueue(UUID packId) {
    synchronized (objectLocker) {
      try {
        putText(packId, PACK_BATCH_FILE, packBatchTextList.get(packId) + "EOF\n");
        host.setParameter("resource_type_num", AbciResourceSelector.getResourceText(queuedJobList.get(packId).size()));
        String resultJson = exec(xsubSubmitCommand(packId));
        for (Job job : queuedJobList.get(packId)) {
          try {
            processXsubSubmit(job, resultJson);
          } catch (Exception e) {
            WarnLogMessage.issue(e);
            job.setState(State.Excepted);
          }
        }
        queuedJobList.remove(packId);
        packBatchTextList.remove(packId);
        packWaitThreadMap.remove(packId);
      } catch (FailedToTransferFileException | FailedToControlRemoteException | RunNotFoundException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  String xsubSubmitCommand(UUID packId) throws FailedToControlRemoteException {
    return xsubCommand(packId) + " " + PACK_BATCH_FILE;
  }

  @Override
  public void submit(Job job) throws RunNotFoundException {
    UUID packId = null;
    PackWaitThread packWaitThread = null;

    synchronized (objectLocker) {
      if (currentPackId == null || !queuedJobList.containsKey(currentPackId)
        || queuedJobList.get(currentPackId).size() >= AbciResourceSelector.getPackSize(remainingJobSize)) {
        packId = UUID.randomUUID();
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

      packId = currentPackId;
      jobPackMap.put(job, packId);
      queuedJobList.get(packId).add(job);
      packWaitThread = packWaitThreadMap.get(packId);
      remainingJobSize -= 1;

      packWaitThread.resetWaitTime();
    }

    job.setState(State.Prepared);
    try {
      prepareJob(job);

      synchronized (objectLocker) {
        packBatchTextList.put(packId, packBatchTextList.get(packId) + getRunDirectory(job.getRun()) + "\n");
        packWaitThread.addReadyJob(job);
      }
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      job.setState(State.Excepted);
    }
  }

  public void putText(UUID packId, String path, String text) throws FailedToTransferFileException {
    try {
      session.putText(text, path, getWorkDirectory(packId).toString());
    } catch (JSchException | FailedToControlRemoteException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  String getWorkDirectory(UUID packId) throws FailedToControlRemoteException {
    Path path = parseHomePath(host.getWorkBaseDirectory()).resolve(RUN_DIR).resolve("pack").resolve(packId.toString());

    createDirectories(path);

    return path.toString();
  }

  String xsubCommand(UUID packId) throws FailedToControlRemoteException {
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(host) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getWorkDirectory(packId) + "'; " +
      "XSUB_TYPE=$XSUB_TYPE $XSUB_COMMAND -p '" + host.getXsubParameters().toString().replaceAll("'", "\\\\'") + "' ";
  }

  HashMap<UUID, String> updatedPackJson = new HashMap<>();
  @Override
  public State update(Job job) throws RunNotFoundException {
    SimulatorRun run = job.getRun();
    UUID packId = jobPackMap.get(job);
    if (!updatedPackJson.containsKey(packId)) {
      updatedPackJson.put(packId, exec(xstatCommand(job)));
    }
    processXstat(job, updatedPackJson.get(packId));
    return run.getState();
  }

  @Override
  public void pollingTask(Host host) {

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) { }

    ArrayList<Job> jobList = Job.getList(host);

    int maximumNumberOfJobs = host.getMaximumNumberOfThreads();

    ArrayList<Job> createdJobList = new ArrayList<>();
    int submittedCount = 0;

    updatedPackJson.clear();

    for (Job job : jobList) {
      try {
        switch (job.getState()) {
          case Created:
            if (createdJobList.size() <= maximumNumberOfJobs) {
              job.getRun();
              createdJobList.add(job);
            }
            break;
          case Prepared:
            submittedCount++;
            break;
          case Submitted:
          case Running:
            State state = update(job);
            if (!(State.Finished.equals(state) || State.Failed.equals(state))) {
              submittedCount++;
            }
            break;
          case Finished:
          case Failed:
          case Excepted:
          case Canceled:
            job.remove();
            break;
          case Cancel:
            cancel(job);
        }
      } catch (RunNotFoundException e) {
        try {
          cancel(job);
        } catch (RunNotFoundException ex) { }
        job.remove();
        WarnLogMessage.issue("SimulatorRun(" + job.getId() + ") is not found; The job was removed." );
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
        return 5;
      }
    }
  }
}
