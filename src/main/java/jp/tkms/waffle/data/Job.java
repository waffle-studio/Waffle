package jp.tkms.waffle.data;

import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.data.util.Sql;
import jp.tkms.waffle.data.util.State;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Job {
  private UUID id = null;
  private String projectName = null;
  private String hostName = null;
  private State state = null;
  private String jobId = "";
  private int errorCount = 0;

  public Job(UUID id, Project project, Host host, State state) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public static Job getInstance(String id) {
    return Main.jobStore.getJob(UUID.fromString(id));
  }

  public static ArrayList<Job> getList() {
    return new ArrayList<>(Main.jobStore.getList());
  }

  public static ArrayList<Job> getList(Host host) {
    return new ArrayList<>(Main.jobStore.getList(host));
  }

  public static boolean hasJob(Host host) {
    return getList(host).size() > 0;
  }

  public static int getNum() {
    return getList().size();
  }

  public static void addRun(SimulatorRun run) {
    Job job = new Job(run.getUuid(), run.getProject(), run.getHost(), run.getState());
    Main.jobStore.register(job);
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
  }

  public void cancel() throws RunNotFoundException {
    if (getRun().isRunning()) {
      setState(State.Cancel);
    }
  }

  public void remove() {
    Main.jobStore.remove(id);
    BrowserMessage.addMessage("updateJobNum(" + getNum() + ");");
  }

  public void setJobId(String jobId) throws RunNotFoundException {
    this.jobId = jobId;
  }

  public void setState(State state) throws RunNotFoundException {
    this.state = state;
    if (getRun() != null) {
      getRun().setState(state);
    }
  }

  public void incrementErrorCount() {
    errorCount += 1;
  }

  public Project getProject() {
    return Project.getInstanceByName(projectName);
  }

  public Host getHost() {
    return Host.getInstanceByName(hostName);
  }

  public SimulatorRun getRun() throws RunNotFoundException {
    return SimulatorRun.getInstance(getProject(), id.toString());
  }

  public String getJobId() {
    return jobId;
  }

  public State getState() {
    return state;
  }

  public int getErrorCount() {
    return errorCount;
  }
}
