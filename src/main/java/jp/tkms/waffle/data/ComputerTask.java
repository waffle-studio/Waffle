package jp.tkms.waffle.data;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.util.State;
import jp.tkms.waffle.exception.OccurredExceptionsException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.submitter.AbstractSubmitter;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;

public interface ComputerTask extends DataDirectory {
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
  void specializedPostProcess(AbstractSubmitter submitter, AbstractJob job) throws OccurredExceptionsException, RunNotFoundException;
  ArrayList<Object> getArguments();
  JSONObject getEnvironments();
  void appendErrorNote(String note);
  void setExitStatus(int exitStatus);
  int getExitStatus();
}
