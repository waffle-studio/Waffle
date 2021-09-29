package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.internal.task.AbstractTask;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.communicator.AbstractSubmitter;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;

public interface ComputerTask extends DataDirectory {
  public static final String PARAMETERS_JSON_FILE = "PARAMETERS" + Constants.EXT_JSON;
  public static final String RESULTS_JSON_FILE = "RESULTS" + Constants.EXT_JSON;
  public static final String ARGUMENTS_JSON_FILE = "ARGUMENTS" + Constants.EXT_JSON;
  public static final String KEY_ENVIRONMENTS = "environments";
  static final String KEY_COMPUTER = "computer";
  static final String KEY_ACTUAL_COMPUTER = "actual_computer";
  public static final String KEY_REMOTE_WORKING_DIR = "remote_directory";
  static final String KEY_JOB_ID = "job_id";
  static final String KEY_CREATED_AT = "created_at";
  static final String KEY_SUBMITTED_AT = "submitted_at";
  static final String KEY_FINISHED_AT = "finished_at";
  static final String KEY_EXIT_STATUS = "exit_status";

  State getState();
  Computer getComputer();
  String getJobId();
  void setJobId(String id);
  Double getRequiredThread();
  Double getRequiredMemory();
  String getCommand();
  boolean isRunning();
  void setRemoteWorkingDirectoryLog(String pathString);
  Computer getActualComputer();
  Path getBasePath();
  Path getBinPath();
  Path getRemoteBinPath();
  void specializedPreProcess(AbstractSubmitter submitter);
  void specializedPostProcess(AbstractSubmitter submitter, AbstractTask job) throws OccurredExceptionsException, RunNotFoundException;
  ArrayList<Object> getArguments();
  JSONObject getEnvironments();
  void appendErrorNote(String note);
  void setExitStatus(int exitStatus);
  int getExitStatus();
}
