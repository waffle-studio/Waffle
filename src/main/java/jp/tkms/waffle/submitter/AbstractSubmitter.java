package jp.tkms.waffle.submitter;

import jp.tkms.waffle.collector.RubyResultCollector;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.extractor.AbstractParameterExtractor;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

abstract public class AbstractSubmitter {
  protected static final String RUN_DIR = "run";
  protected static final String SIMULATOR_DIR = "simulator";
  protected static final String BATCH_FILE = "batch.sh";
  protected static final String ARGUMENTS_FILE = "arguments.txt";
  protected static final String ENVIRONMENTS_FILE = "environments.txt";
  protected static final String EXIT_STATUS_FILE = "exit_status.log";

  abstract public AbstractSubmitter connect(boolean retry);
  abstract String getRunDirectory(SimulatorRun run);
  abstract String getWorkDirectory(SimulatorRun run);
  abstract String getSimulatorBinDirectory(SimulatorRun run);
  abstract void prepareSubmission(SimulatorRun run);
  abstract String exec(String command);
  abstract int getExitStatus(SimulatorRun run) throws Exception;
  abstract void postProcess(SimulatorRun run);
  abstract public void close();
  abstract public void putText(SimulatorRun run, String path, String text);
  abstract public String getFileContents(SimulatorRun run, String path);
  abstract public JSONObject defaultParameters(Host host);

  public AbstractSubmitter connect() {
    return connect(true);
  }

  public static AbstractSubmitter getInstance(Host host) {
    AbstractSubmitter submitter = null;
    if (host.isLocal()) {
      submitter = new LocalSubmitter();
    } else if (host.getName().equals("ABCI")) {
      submitter = new AbciSubmitter(host);
    } else {
      submitter = new SshSubmitter(host);
    }
    return submitter;
  }

  public void submit(Job job) {
    SimulatorRun run = job.getRun();

    putText(run, BATCH_FILE, makeBatchFileText(run));

    for (String extractorName : run.getSimulator().getExtractorNameList()) {
      new RubyParameterExtractor().extract(this, run, extractorName);
    }

    putText(run, EXIT_STATUS_FILE, "-2");
    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    prepareSubmission(run);

    processXsubSubmit(job, exec(xsubSubmitCommand(job)));
  }

  public State update(Job job) {
    SimulatorRun run = job.getRun();
    processXstat(job, exec(xstatCommand(job)));
    return run.getState();
  }

  String makeBatchFileText(SimulatorRun run) {
    return "#!/bin/sh\n" +
      "\n" +
      "export REMOTE='" + getSimulatorBinDirectory(run) + "'\n" +
      "chmod a+x '" + getSimulatorBinDirectory(run) + "/" + run.getSimulator().getSimulationCommand() + "'\n" +
      "export PATH=\"$REMOTE:$PATH\"\n" +
      "mkdir -p " + getWorkDirectory(run) + "\n" +
      "BATCH_WORKING_DIR=`pwd`\n" +
      "cd " + getWorkDirectory(run) + "\n" +
      "cat ${BATCH_WORKING_DIR}/" + ARGUMENTS_FILE + " | xargs -d '\\n' " +
      run.getSimulator().getSimulationCommand() + " >${BATCH_WORKING_DIR}/stdout.txt 2>${BATCH_WORKING_DIR}/stderr.txt\n" +
      "EXIT_STATUS=$?\n" +
      "cd ${BATCH_WORKING_DIR}\n" +
      "echo ${EXIT_STATUS} > " + EXIT_STATUS_FILE + "\n" +
      "";
  }

  String makeArgumentFileText(SimulatorRun run) {
    String text = "";
    for (Object o : run.getArguments()) {
      text += o.toString() + "\n";
    }
    return text;
  }

  String makeEnvironmentFileText(SimulatorRun run) {
    String text = "";
    for (Map.Entry<String, Object> entry : run.getEnvironments().toMap().entrySet()) {
      text += "export " + entry.getKey() + "='" + entry.getValue().toString() + "'\n";
    }
    return text;
  }

  String xsubSubmitCommand(Job job) {
    //return xsubCommand(job) + " -d '" + getRunDirectory(job.getRun()) + "' " + BATCH_FILE;
    return xsubCommand(job) + " " + BATCH_FILE;
  }

  String xsubCommand(Job job) {
    Host host = job.getHost();
    return "XSUB_COMMAND=`which " + getXsubBinDirectory(host) + "xsub`; " +
      "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '" + getRunDirectory(job.getRun()) + "'; " +
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

  void processXsubSubmit(Job job, String json) {
    try {
      JSONObject object = new JSONObject(json);
      String jobId = object.getString("job_id");
      job.setJobId(jobId);
      job.getRun().setState(State.Submitted);
      BrowserMessage.info("Run(" + job.getRun().getShortId() + ") was submitted");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(json);
    }
  }

  void processXstat(Job job, String json) {
    BrowserMessage.info("Run(" + job.getRun().getShortId() + ") will be checked");
    JSONObject object = new JSONObject(json);
    try {
      String status = object.getString("status");
      switch (status) {
        case "running" :
          job.getRun().setState(State.Running);
          break;
        case "finished" :
          int exitStatus = -1;
          try {
            exitStatus = getExitStatus(job.getRun());
          } catch (Exception e) {
            /*
            if (job.getErrorCount() >= 5) {
              System.err.println(getRunDirectory(job.getRun()) + "/" + EXIT_STATUS_FILE);
            } else {
              job.incrementErrorCount();
              break;
            }
            */
            System.err.println(getRunDirectory(job.getRun()) + "/" + EXIT_STATUS_FILE);
          }

          if (exitStatus == 0) {
            BrowserMessage.info("Run(" + job.getRun().getShortId() + ") results will be collected");
            job.getRun().setState(State.Finished);
            for (String collectorName : job.getRun().getSimulator().getCollectorNameList()) {
              new RubyResultCollector().collect(this, job.getRun(), collectorName);
            }
          } else {
            job.getRun().setState(State.Failed);
          }

          job.getRun().setExitStatus(exitStatus);
          job.remove();
          postProcess(job.getRun());
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void processXdel(Job job, String json) {
  }

  String getContentsPath(SimulatorRun run, String path) {
    return getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + path;
  }

  public static String getXsubBinDirectory(Host host) {
    return (host.getXsubDirectory().equals("") ? "":
      host.getXsubDirectory() + host.getDirectorySeparetor() + "bin" + host.getDirectorySeparetor()
      );
  }

  public static JSONObject getXsubTemplate(Host host, boolean retry) throws RuntimeException {
    AbstractSubmitter submitter = getInstance(host).connect(retry);
    JSONObject jsonObject = new JSONObject();
    String command = "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE " +
      getXsubBinDirectory(host) + "xsub -t";
    String json = submitter.exec(command);
    if (json != null) {
      try {
        jsonObject = new JSONObject(json);
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse JSON");
      }
    }
    return jsonObject;
  }

  public static JSONObject getParameters(Host host) {
    AbstractSubmitter submitter = getInstance(host);
    JSONObject jsonObject = submitter.defaultParameters(host);
    return jsonObject;
  }

  public void pollingTask(Host host) {
    ArrayList<Job> jobList = Job.getList(host);

    int maximumNumberOfJobs = host.getMaximumNumberOfJobs();

    ArrayList<Job> queuedJobList = new ArrayList<>();
    int submittedCount = 0;

    for (Job job : jobList) {
      SimulatorRun run = job.getRun();
      switch (run.getState()) {
        case Created:
          if (queuedJobList.size() < maximumNumberOfJobs) {
            queuedJobList.add(job);
          }
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
          job.remove();
      }
    }

    for (Job job : queuedJobList) {
      if (submittedCount < maximumNumberOfJobs) {
        submit(job);
        submittedCount++;
      }
    }
  }

  public void hibernate() {

  }
}
