package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

abstract public class AbstractSubmitter {
  protected static final String RUN_DIR = "run";
  protected static final String INNER_WORK_DIR = "WORK";
  protected static final String BATCH_FILE = "batch.sh";
  protected static final String ARGUMENTS_FILE = "arguments.txt";
  protected static final String ENVIRONMENTS_FILE = "environments.txt";
  protected static final String EXIT_STATUS_FILE = "exit_status.log";

  abstract String getWorkDirectory(Run run);
  abstract void prepare(Run run);
  abstract String exec(Run run, String command);
  abstract int getExitStatus(Run run);
  abstract void postProcess(Run run);
  abstract public void close();
  abstract public String getFileContents(Run run, String path);

  public static AbstractSubmitter getInstance(Host host) {
    AbstractSubmitter submitter = null;
    if (host.isLocal()) {
      submitter = new LocalSubmitter();
    } else {
      submitter = new SshSubmitter(host);
    }
    return submitter;
  }

  public void submit(Job job) {
    Run run = job.getRun();
    prepare(run);
    processXsubSubmit(job, exec(run, xsubSubmitCommand(job)));
  }

  public Run.State update(Job job) {
    Run run = job.getRun();
    processXstat(job, exec(run, xstatCommand(job)));
    return run.getState();
  }

  String makeBatchFileText(Run run) {
    return "#!/bin/sh\n" +
      "\n" +
      "mkdir " + INNER_WORK_DIR + "\n" +
      "BATCH_WORKING_DIR=`pwd`\n" +
      "cd " + INNER_WORK_DIR + "\n" +
      "cat ../" + ARGUMENTS_FILE + " | xargs -d '\n' " +
      run.getSimulator().getSimulationCommand() + " >${BATCH_WORKING_DIR}/stdout.txt 2>${BATCH_WORKING_DIR}/stderr.txt\n" +
      "EXIT_STATUS=$?\n" +
      "cd ${BATCH_WORKING_DIR}\n" +
      "echo ${EXIT_STATUS} > " + EXIT_STATUS_FILE + "\n" +
      "";
  }

  String makeArgumentFileText(Run run) {
    String text = "";
    for (Object o : run.getArguments()) {
      text += o.toString() + "\n";
    }
    return text;
  }

  String makeEnvironmentFileText(Run run) {
    String text = "";
    for (Map.Entry<String, Object> entry : run.getEnvironments().toMap().entrySet()) {
      text += "export " + entry.getKey() + "='" + entry.getValue().toString() + "'\n";
    }
    return text;
  }

  String xsubSubmitCommand(Job job) {
    //return xsubCommand(job) + " -d '" + getWorkDirectory(job.getRun()) + "' " + BATCH_FILE;
    return xsubCommand(job) + " " + BATCH_FILE;
  }

  String xsubCommand(Job job) {
    Host host = job.getHost();
    return "EP=`pwd`; if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; cd '"
      + getWorkDirectory(job.getRun())
      + "'; XSUB_TYPE=$XSUB_TYPE $EP" + host.getDirectorySeparetor()
      + host.getXsubDirectory() + host.getDirectorySeparetor() + "bin" + host.getDirectorySeparetor() + "xsub ";
  }

  String xstatCommand(Job job) {
    Host host = job.getHost();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + host.getXsubDirectory() + host.getDirectorySeparetor()
      + "bin" + host.getDirectorySeparetor() + "xstat " + job.getJobId();
  }

  String xdelCommand(Job job) {
    Host host = job.getHost();
    return "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE "
      + host.getXsubDirectory() + host.getDirectorySeparetor()
      + "bin" + host.getDirectorySeparetor() + "xdel " + job.getJobId();
  }

  void processXsubSubmit(Job job, String json) {
    JSONObject object = new JSONObject(json);
    String jobId = object.getString("job_id");
    job.setJobId(jobId);
    job.getRun().setState(Run.State.Submitted);
  }

  void processXstat(Job job, String json) {
    JSONObject object = new JSONObject(json);
    try {
      String status = object.getString("status");
      switch (status) {
        case "running" :
          job.getRun().setState(Run.State.Running);
          break;
        case "finished" :
          int exitStatus = getExitStatus(job.getRun());
          if (exitStatus == 0) {
            job.getRun().setState(Run.State.Finished);
            for ( ResultCollector collector : ResultCollector.getList(job.getRun().getSimulator()) ) {
              collector.getResultCollector().collect(job.getRun(), collector);
            }
          } else {
            job.getRun().setState(Run.State.Failed);
          }
          job.getRun().setExitStatus(exitStatus);
          job.remove();
          postProcess(job.getRun());
          break;
      }
    } catch (Exception e) {}
  }

  void processXdel(Job job, String json) {
  }

  String getContentsPath(Run run, String path) {
    return getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + INNER_WORK_DIR
       + run.getHost().getDirectorySeparetor() + path;
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
