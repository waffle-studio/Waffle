package jp.tkms.waffle.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.InfoLogMessage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class JobStore {
  private LinkedHashMap<String, Job> jobMap;
  private LinkedHashMap<String, ArrayList<Job>> hostJobListMap;

  public JobStore() {
    jobMap = new LinkedHashMap<>();
    hostJobListMap = new LinkedHashMap<>();
  }

  public Job getJob(UUID id) {
    synchronized (jobMap) {
      return jobMap.get(id.toString());
    }
  }

  public ArrayList<Job> getList() {
    synchronized (jobMap) {
      return new ArrayList<>(jobMap.values());
    }
  }

  public ArrayList<Job> getList(Host host) {
    synchronized (jobMap) {
      ArrayList list = hostJobListMap.get(host.getName());
      if (list == null) {
        list = new ArrayList();
        hostJobListMap.put(host.getName(), list);
      }
      return list;
    }
  }

  public boolean contains(UUID id) {
    synchronized (jobMap) {
      return jobMap.containsKey(id);
    }
  }

  public void register(Job job) {
    synchronized (jobMap) {
      jobMap.put(job.getId(), job);
      getList(job.getHost()).add(job);
    }
  }

  public void remove(String id) {
    if (id == null) {
      return;
    }
    synchronized (jobMap) {
      Job removedJob = jobMap.remove(id);
      if (removedJob != null) {
        getList(removedJob.getHost()).remove(removedJob);
      }
    }
  }

  public void save() throws IOException {
    InfoLogMessage.issue("Waiting for the job store to release");
    synchronized (jobMap) {
      GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(getFilePath().toFile()));
      Kryo kryo = new Kryo();
      Output output = new Output(outputStream);
      kryo.writeObject(output, this);
      output.flush();
      output.close();
      InfoLogMessage.issue("The snapshot of job store saved");
    }
  }

  public static JobStore load() {
    JobStore data = null;
    try {
      GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(getFilePath().toFile()));
      Kryo kryo = new Kryo();
      Input input = new Input(inputStream);
      data = kryo.readObject(input, JobStore.class);
      input.close();
    } catch (Exception e) {
      data = new JobStore();
    }
    return data;
  }

  public static Path getFilePath() {
    return Constants.WORK_DIR.resolve(".jobstore.dat");
  }
}
