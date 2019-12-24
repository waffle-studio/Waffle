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

public class AbciSubmitter extends SshSubmitter {

  static UUID packId = null;
  static ArrayList<Job> queuedJobList = new ArrayList<>();
  static HashMap<Job, UUID> jobPackMap = new HashMap<>();

  public AbciSubmitter(Host host) {
    super(host);
  }

  private void submitQueue() {
    for (Job job : queuedJobList) {

    }
  }

  String xsubSubmitCommand(Job job) {
    //return xsubCommand(job) + " -d '" + getWorkDirectory(job.getRun()) + "' " + BATCH_FILE;
    return xsubCommand(job) + " " + BATCH_FILE;
  }

  @Override
  public synchronized void submit(Job job) {
    if (queuedJobList.size() >= 40) {
      submitQueue();
      packId = null;
    }

    if (packId == null) {
      packId = UUID.randomUUID();
      queuedJobList.clear();
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

    prepareSubmission(run);

    //processXsubSubmit(job, exec(xsubSubmitCommand(job)));
  }

  @Override
  String getWorkDirectory(Run run) {
    Host host = run.getHost();
    String pathString = host.getWorkBaseDirectory() + host.getDirectorySeparetor()
      + RUN_DIR + host.getDirectorySeparetor() + run.getId();

    try {
      session.mkdir(pathString, "~/");
    } catch (JSchException e) {
      e.printStackTrace();
    }

    return toAbsoluteHomePath(pathString);
  }

  String xsubCommand(Job job) {
    Host host = job.getHost();
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(host) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getWorkDirectory(job.getRun()) + "'; " +
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
          if (queuedJobList.size() < maximumNumberOfJobs) {
            queuedJobList.add(job);
          }
          break;
        case Submitted:
        case Running:
        case Finished:
          Run.State state = update(job);
          if (!Run.State.Finished.equals(state)) {
            submittedCount++;
          }
          break;
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
