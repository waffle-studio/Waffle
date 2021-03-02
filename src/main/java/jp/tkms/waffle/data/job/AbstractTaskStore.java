package jp.tkms.waffle.data.job;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.WaffleId;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public abstract class AbstractTaskStore<T extends AbstractJob> {

  private LinkedHashMap<WaffleId, T> jobMap;
  private LinkedHashMap<String, ArrayList<T>> computerJobListMap;

  protected AbstractTaskStore() {
    jobMap = new LinkedHashMap<>();
    computerJobListMap = new LinkedHashMap<>();
  }

  public T getJob(WaffleId id) {
    synchronized (jobMap) {
      return jobMap.get(id);
    }
  }

  public ArrayList<T> getList() {
    synchronized (jobMap) {
      return new ArrayList<>(jobMap.values());
    }
  }

  public ArrayList<T> getList(Computer computer) {
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

  public void register(T job) {
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
      AbstractJob removedJob = jobMap.remove(id);
      if (removedJob != null) {
        getList(removedJob.getComputer()).remove(removedJob);
      }
    }
  }

  protected static void load(AbstractTaskStore instance, Path directory, JobFactoryFunction<WaffleId, Path, String, AbstractJob> factory) {
    InfoLogMessage.issue("Loading the snapshot of job store");

    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    int fileCount = 0;
    for (File computerDir : directory.toFile().listFiles()) {
      if (computerDir.isDirectory()) {
        Computer computer = Computer.getInstance(computerDir.getName());
        if (computer != null) {
          for (Object object : Arrays.stream(computerDir.listFiles()).sorted().toArray()) {
            File file = (File) object;

            try {
              Path jsonPath = file.toPath();
              if (jsonPath != null) {
                JSONObject jsonObject = new JSONObject(Files.readString(jsonPath));
                WaffleId id = WaffleId.valueOf(jsonObject.getLong(SystemTaskJob.KEY_ID));
                Path path = Paths.get(jsonObject.getString(SystemTaskJob.KEY_PATH));
                String computerName = computerDir.getName();
                instance.register(factory.apply(id, path, computerName));
                fileCount += 1;
              }
            } catch (Exception e) {
              file.delete();
              WarnLogMessage.issue(file.toPath().toString() + " is broken : " + e.getMessage());
            }
          }
        }
      }
    }

    InfoLogMessage.issue(fileCount + " job loaded");
  }

  @FunctionalInterface
  public interface JobFactoryFunction<W, P, S, R> {
    R apply(W id, P path, S computerName);
  }
}
