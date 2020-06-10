package jp.tkms.waffle.submitter;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.collector.RubyResultCollector;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.data.log.WarnLogMessage;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.extractor.RubyParameterExtractor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

abstract public class AbstractSubmitter {
  protected static final String RUN_DIR = "run";
  protected static final String SIMULATOR_DIR = "simulator";
  protected static final String BATCH_FILE = "batch.sh";
  protected static final String ARGUMENTS_FILE = "arguments.txt";
  protected static final String EXIT_STATUS_FILE = "exit_status.log";

  abstract public AbstractSubmitter connect(boolean retry);
  abstract String getRunDirectory(SimulatorRun run);
  abstract String getWorkDirectory(SimulatorRun run);
  abstract String getSimulatorBinDirectory(SimulatorRun run);
  abstract void prepareSubmission(SimulatorRun run);
  abstract String exec(String command);
  abstract boolean exists(String path);
  abstract int getExitStatus(SimulatorRun run) throws Exception;
  abstract void postProcess(SimulatorRun run);
  abstract public void close();
  abstract public void putText(SimulatorRun run, String path, String text);
  abstract public String getFileContents(SimulatorRun run, String path);
  abstract public JSONObject defaultParameters(Host host);
  abstract void transferFile(Path localPath, String remotePath);
  abstract void transferFile(String remotePath, Path localPath);

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

    run.getSimulator().updateVersionId();

    putText(run, BATCH_FILE, makeBatchFileText(run));
    putText(run, EXIT_STATUS_FILE, "-2");

    try {
      for (String extractorName : run.getSimulator().getExtractorNameList()) {
        new RubyParameterExtractor().extract(this, run, extractorName);
      }
    } catch (Exception e) {
      WarnLogMessage.issue(e);
      job.getRun().setState(State.Excepted);
      return;
    }
    putText(run, ARGUMENTS_FILE, makeArgumentFileText(run));
    //putText(run, ENVIRONMENTS_FILE, makeEnvironmentFileText(run));

    if (! exists(Paths.get(getSimulatorBinDirectory(run)).toAbsolutePath().toString())) {
      Path binPath = run.getSimulator().getBinDirectory().toAbsolutePath();
      transferFile(binPath, Paths.get(getSimulatorBinDirectory(run)).toAbsolutePath().toString());
    }

    prepareSubmission(run);

    processXsubSubmit(job, exec(xsubSubmitCommand(job)));
  }

  public State update(Job job) {
    SimulatorRun run = job.getRun();
    processXstat(job, exec(xstatCommand(job)));
    return run.getState();
  }

  String makeBatchFileText(SimulatorRun run) {
    JSONArray localSharedList = run.getLocalSharedList();

    String text = "#!/bin/sh\n" +
      "\n" +
      "export WAFFLE_REMOTE='" + getSimulatorBinDirectory(run) + "'\n" +
      "export WAFFLE_BATCH_WORKING_DIR=`pwd`\n" +
      "mkdir -p " + getWorkDirectory(run) +"\n" +
      "cd " + getWorkDirectory(run) + "\n" +
      "export WAFFLE_WORKING_DIR=`pwd`\n" +
      "cd '" + getSimulatorBinDirectory(run) + "'\n" +
      "chmod a+x '" + run.getSimulator().getSimulationCommand() + "' >/dev/null 2>&1\n" +
      "find . -type d | xargs -n 1 -I{1} sh -c 'mkdir -p \"${WAFFLE_WORKING_DIR}/{1}\";find {1} -maxdepth 1 -type f | xargs -n 1 -I{2} ln -s \"`pwd`/{2}\" \"${WAFFLE_WORKING_DIR}/{1}/\"'\n" +
      "cd ${WAFFLE_BATCH_WORKING_DIR}\n" +
      "export WAFFLE_LOCAL_SHARED=\"" + run.getHost().getWorkBaseDirectory() + "/local_shared/" + run.getProject().getId() + "\"\n" +
      "mkdir -p \"$WAFFLE_LOCAL_SHARED\"\n" +
      "cd \"${WAFFLE_WORKING_DIR}\"\n";

    for (int i = 0; i < localSharedList.length(); i++) {
      JSONArray a = localSharedList.getJSONArray(i);
      text += makeLocalSharingPreCommandText(a.getString(0), a.getString(1));
    }

    text += "\n" + "cat ${WAFFLE_BATCH_WORKING_DIR}/" + ARGUMENTS_FILE + " | xargs -d '\\n' " +
      run.getSimulator().getSimulationCommand() + " >${WAFFLE_BATCH_WORKING_DIR}/" + Constants.STDOUT_FILE + " 2>${WAFFLE_BATCH_WORKING_DIR}/" + Constants.STDERR_FILE + "\n" +
      "EXIT_STATUS=$?\n";

    for (int i = 0; i < localSharedList.length(); i++) {
      JSONArray a = localSharedList.getJSONArray(i);
      text += makeLocalSharingPostCommandText(a.getString(0), a.getString(1));
    }

    text += "\n" + "cd ${WAFFLE_BATCH_WORKING_DIR}\n" +
      "echo ${EXIT_STATUS} > " + EXIT_STATUS_FILE + "\n" +
      "\n";

    return text;
  }

  String makeLocalSharingPreCommandText(String key, String remote) {
    return "mkdir -p `dirname \"" + remote + "\"`;if [ -e \"${WAFFLE_LOCAL_SHARED}/" + key + "\" ]; then ln -fs \"${WAFFLE_LOCAL_SHARED}/" + key + "\" \"" + remote + "\"; else echo \"" + key + "\" >> \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; fi\n";
  }

  String makeLocalSharingPostCommandText(String key, String remote) {
    return "if grep \"^" + key + "$\" \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; then mv \"" + remote + "\" \"${WAFFLE_LOCAL_SHARED}/" + key + "\"; ln -fs \"${WAFFLE_LOCAL_SHARED}/"  + key + "\" \"" + remote + "\" ;fi";
  }

  String makeArgumentFileText(SimulatorRun run) {
    String text = "";
    for (Object o : run.getArguments()) {
      text += o.toString() + "\n";
    }
    return text;
  }

  String makeEnvironmentCommandText(SimulatorRun run) {
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
      InfoLogMessage.issue("Run(" + job.getRun().getShortId() + ") was submitted");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(json);
    }
  }

  void processXstat(Job job, String json) {
    InfoLogMessage.issue("Run(" + job.getRun().getShortId() + ") will be checked");
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

          transferFile(Paths.get(getRunDirectory(job.getRun())).resolve(Constants.STDOUT_FILE).toString(), job.getRun().getDirectoryPath().resolve(Constants.STDOUT_FILE));
          transferFile(Paths.get(getRunDirectory(job.getRun())).resolve(Constants.STDERR_FILE).toString(), job.getRun().getDirectoryPath().resolve(Constants.STDERR_FILE));

          if (exitStatus == 0) {
            InfoLogMessage.issue("Run(" + job.getRun().getShortId() + ") results will be collected");

            try {
              for (String collectorName : job.getRun().getSimulator().getCollectorNameList()) {
                new RubyResultCollector().collect(this, job.getRun(), collectorName);
              }

              job.getRun().setState(State.Finished);
            } catch (Exception e) {
              job.getRun().setState(State.Excepted);
              WarnLogMessage.issue(e);
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
          if (!(State.Finished.equals(state)
            || State.Failed.equals(state)
            || State.Excepted.equals(state)
            || State.Canceled.equals(state))) {
            submittedCount++;
          }
          break;
        case Finished:
        case Failed:
        case Excepted:
        case Canceled:
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

  /*
  public boolean stageIn(SimulatorRun run, String name, String remote) {
    if (updated) {
      tranfar file to remote shared dir from local shared dir
    }
    soft copy to run dir from remote shared dir
  }

  public boolean stageOut(SimulatorRun run, String name, String remote) {

  }
   */
}
