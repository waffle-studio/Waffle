package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.*;
import jp.tkms.waffle.extractor.AbstractParameterExtractor;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import org.json.JSONObject;

import java.util.Map;

abstract public class AbstractSubmitter {
  protected static final String RUN_DIR = "run";
  protected static final String SIMULATOR_DIR = "simulator";
  protected static final String INNER_WORK_DIR = "WORK";
  protected static final String BATCH_FILE = "batch.sh";
  protected static final String ARGUMENTS_FILE = "arguments.txt";
  protected static final String ENVIRONMENTS_FILE = "environments.txt";
  protected static final String EXIT_STATUS_FILE = "exit_status.log";

  abstract public AbstractSubmitter connect();
  abstract String getWorkDirectory(Run run);
  abstract String getSimulatorBinDirectory(Run run);
  abstract void prepareSubmission(Run run);
  abstract String exec(String command);
  abstract int getExitStatus(Run run);
  abstract void postProcess(Run run);
  abstract public void close();
  abstract public void putText(Run run, String path, String text);
  abstract public String getFileContents(Run run, String path);
  abstract public JSONObject defaultParameters(Host host);

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

    putText(run, BATCH_FILE, makeBatchFileText(run));

    for (ParameterExtractor extractor : ParameterExtractor.getList(run.getSimulator())) {
      AbstractParameterExtractor instance = AbstractParameterExtractor.getInstance(RubyParameterExtractor.class.getCanonicalName());
      instance.extract(run, extractor, this);
    }

    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    prepareSubmission(run);

    processXsubSubmit(job, exec(xsubSubmitCommand(job)));
  }

  public Run.State update(Job job) {
    Run run = job.getRun();
    processXstat(job, exec(xstatCommand(job)));
    return run.getState();
  }

  String makeBatchFileText(Run run) {
    return "#!/bin/sh\n" +
      "\n" +
      "export PATH='" + getSimulatorBinDirectory(run) + "':$PATH\n" +
      "mkdir " + INNER_WORK_DIR + "\n" +
      "BATCH_WORKING_DIR=`pwd`\n" +
      "cd " + INNER_WORK_DIR + "\n" +
      "cat ../" + ARGUMENTS_FILE + " | xargs -d '\\n' " +
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
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void processXdel(Job job, String json) {
  }

  String getContentsPath(Run run, String path) {
    return getWorkDirectory(run) + run.getHost().getDirectorySeparetor() + INNER_WORK_DIR
       + run.getHost().getDirectorySeparetor() + path;
  }

  public static String getXsubBinDirectory(Host host) {
    return (host.getXsubDirectory().equals("") ? "":
      host.getXsubDirectory() + host.getDirectorySeparetor() + "bin" + host.getDirectorySeparetor()
      );
  }

  public static JSONObject getXsubTemplate(Host host) {
    AbstractSubmitter submitter = getInstance(host).connect();
    JSONObject jsonObject = new JSONObject();
    String command = "if test ! $XSUB_TYPE; then XSUB_TYPE=None; fi; XSUB_TYPE=$XSUB_TYPE " +
      getXsubBinDirectory(host) + "xsub -t";
    String json = submitter.exec(command);
    if (json != null) {
      jsonObject = new JSONObject(json);
    }
    return jsonObject;
  }

  public static JSONObject getParameters(Host host) {
    AbstractSubmitter submitter = getInstance(host);
    JSONObject jsonObject = submitter.defaultParameters(host);
    return jsonObject;
  }

  public static JSONObject getParametersWithXsubParameter(Host host) {
    AbstractSubmitter submitter = getInstance(host);
    JSONObject jsonObject = submitter.defaultParameters(host);

    try {
      JSONObject object = getXsubTemplate(host).getJSONObject("parameters");
      for (String key : object.toMap().keySet()) {
        jsonObject.put(key, object.getJSONObject(key).get("default"));
      }
    } catch (Exception e) {}

    return jsonObject;
  }

  public static JSONObject getXsubParameter(Host host) {
    JSONObject jsonObject = new JSONObject();

    try {
      JSONObject object = getXsubTemplate(host).getJSONObject("parameters");
      for (String key : object.toMap().keySet()) {
        jsonObject.put(key, object.getJSONObject(key).get("default"));
      }
    } catch (Exception e) {}

    return jsonObject;
  }
}
