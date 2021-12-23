package jp.tkms.waffle.data.internal.task;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.WaffleId;
import jp.tkms.waffle.data.util.WrappedJson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public abstract class AbstractTaskStore<T extends AbstractTask> {
  private LinkedHashMap<WaffleId, T> taskMap;
  private LinkedHashMap<String, ArrayList<T>> computerTaskListMap;

  protected abstract String getName();

  protected AbstractTaskStore() {
    taskMap = new LinkedHashMap<>();
    computerTaskListMap = new LinkedHashMap<>();
  }

  public T getTask(WaffleId id) {
    synchronized (taskMap) {
      return taskMap.get(id);
    }
  }

  public ArrayList<T> getList() {
    synchronized (taskMap) {
      return new ArrayList<>(taskMap.values());
    }
  }

  public ArrayList<T> getList(Computer computer) {
    synchronized (taskMap) {
      ArrayList list = computerTaskListMap.get(computer.getName());
      if (list == null) {
        list = new ArrayList();
        computerTaskListMap.put(computer.getName(), list);
      }
      return list;
    }
  }

  public boolean contains(WaffleId id) {
    synchronized (taskMap) {
      return taskMap.containsKey(id);
    }
  }

  public void register(T job) {
    synchronized (taskMap) {
      taskMap.put(job.getId(), job);
      getList(job.getComputer()).add(job);
    }
  }

  public void remove(WaffleId id) {
    if (id == null) {
      return;
    }
    synchronized (taskMap) {
      AbstractTask removedJob = taskMap.remove(id);
      if (removedJob != null) {
        getList(removedJob.getComputer()).remove(removedJob);
      }
    }
  }

  protected static void load(AbstractTaskStore instance, Path directory, TaskFactoryFunction<WaffleId, Path, String, AbstractTask> factory) {
    InfoLogMessage.issue("Loading the snapshot of " + instance.getName() + " job store");

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
                WrappedJson jsonObject = new WrappedJson(Files.readString(jsonPath));
                WaffleId id = WaffleId.valueOf(jsonObject.getLong(SystemTask.KEY_ID, null));
                Path path = Paths.get(jsonObject.getString(SystemTask.KEY_PATH, null));
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

    InfoLogMessage.issue(fileCount + " " + instance.getName() + " jobs loaded");
  }

  @FunctionalInterface
  public interface TaskFactoryFunction<W, P, S, R> {
    R apply(W id, P path, S computerName);
  }
}
