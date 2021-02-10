package jp.tkms.waffle.data.job;

import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.DataDirectory;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.exception.RunNotFoundException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JobStore {
  public static final String TASK = "TASK";

  private LinkedHashMap<WaffleId, Job> jobMap;
  private LinkedHashMap<String, ArrayList<Job>> computerJobListMap;

  public JobStore() {
    jobMap = new LinkedHashMap<>();
    computerJobListMap = new LinkedHashMap<>();
  }

  public Job getJob(WaffleId id) {
    synchronized (jobMap) {
      return jobMap.get(id.toString());
    }
  }

  public ArrayList<Job> getList() {
    synchronized (jobMap) {
      return new ArrayList<>(jobMap.values());
    }
  }

  public ArrayList<Job> getList(Computer computer) {
    synchronized (jobMap) {
      ArrayList list = computerJobListMap.get(computer.getName());
      if (list == null) {
        list = new ArrayList();
        computerJobListMap.put(computer.getName(), list);
      }
      return list;
    }
  }

  public boolean contains(WaffleId id) {
    synchronized (jobMap) {
      return jobMap.containsKey(id);
    }
  }

  public void register(Job job) {
    synchronized (jobMap) {
      jobMap.put(job.getId(), job);
      getList(job.getComputer()).add(job);
    }
  }

  public void remove(WaffleId id) {
    if (id == null) {
      return;
    }
    synchronized (jobMap) {
      Job removedJob = jobMap.remove(id);
      if (removedJob != null) {
        getList(removedJob.getComputer()).remove(removedJob);
      }
    }
  }

  public static JobStore load() {
    InfoLogMessage.issue("Loading the snapshot of job store");
    JobStore jobStore = new JobStore();

    try {
      Files.createDirectories(getDirectoryPath());
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    for (File computerDir : getDirectoryPath().toFile().listFiles()) {
      if (computerDir.isDirectory()) {
        Computer computer = Computer.getInstance(computerDir.getName());
        if (computer != null) {
          Arrays.stream(computerDir.listFiles()).sorted().forEach(file -> {
            try {
              JSONObject jsonObject = new JSONObject(Files.readString(file.toPath()));
              WaffleId id = WaffleId.valueOf(jsonObject.getLong(Job.KEY_ID));
              Path path = Paths.get(jsonObject.getString(Job.KEY_PATH));
              String computerName = computerDir.getName();
              jobStore.register(new Job(id, path, computerName));
            } catch (Exception e) {
              WarnLogMessage.issue(file.toPath().toString() + " is broken : " + e.getMessage());
            }
          });
        }
      }
    }

    return jobStore;
  }

  public static Path getDirectoryPath() {
    return Constants.WORK_DIR.resolve(Constants.DOT_INTERNAL).resolve(TASK);
  }
}
