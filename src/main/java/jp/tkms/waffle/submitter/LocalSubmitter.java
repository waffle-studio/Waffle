package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.Job;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.log.InfoLogMessage;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalSubmitter extends AbstractSubmitter {
  @Override
  public AbstractSubmitter connect(boolean retry) {
    return this;
  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public String getRunDirectory(SimulatorRun run) {
    Host host = run.getActualHost();
    String pathString = host.getWorkBaseDirectory() + File.separator
      + RUN_DIR + File.separator + run.getId();

    try {
      Files.createDirectories(Paths.get(pathString + File.separator));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return toAbsoluteHomePath(pathString);
  }

  @Override
  public String getWorkDirectory(SimulatorRun run) {
    return run.getWorkPath().toString();
  }

  @Override
  String getSimulatorBinDirectory(Job job) {
    //String pathString = host.getWorkBaseDirectory() + sep + SIMULATOR_DIR + sep+ run.getSimulator().getId() + sep + Simulator.KEY_REMOTE;
    String pathString = job.getHost().getWorkBaseDirectory() + File.separator + SIMULATOR_DIR + File.separator + job.getRun().getSimulator().getVersionId();

    return toAbsoluteHomePath(pathString);
  }

  @Override
  void prepareSubmission(Job job) {
    /*
     * preparing a host's simulator working directory is not needed
     *
    try {
      Files.createDirectories(Paths.get(getSimulatorBinDirectory(run)));
      session.scp(run.getSimulator().getBinDirectoryLocation().toFile(), getSimulatorBinDirectory(run), "/tmp");
    } catch (IOException e) {
      e.printStackTrace();
    }
     */
    InfoLogMessage.issue(job.getRun(), "was prepared");
  }

  @Override
  public String exec(String command) {
    String result = "";
    ProcessBuilder p = new ProcessBuilder("sh", "-c", command);
    p.redirectErrorStream(true);

    try {
      Process process = p.start();

      try (BufferedReader r
             = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
        String line;
        while ((line = r.readLine()) != null) {
          result += line + "\n";
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  @Override
  boolean exists(String path) {
    return Files.exists(Paths.get(path));
  }

  @Override
  void postProcess(Job job) {
    try {
      //deleteDirectory(getRunDirectory(run));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {

  }

  @Override
  public void putText(Job job, String path, String text) {
    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter(
        new FileWriter(getRunDirectory(job.getRun()) + File.separator + path)
      ));
      pw.println(text);
      pw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getFileContents(SimulatorRun run, String path){
    return exec("cat " + getContentsPath(run, path));
  }

  @Override
  public JSONObject getDefaultParameters(Host host) {
    return new JSONObject();
  }

  @Override
  public void transferFile(Path localPath, String remotePath) throws FailedToTransferFileException {
    try {
      Path remote = Paths.get(remotePath);
      Files.createDirectories(remote.getParent());
      if (Files.isDirectory(localPath)) {
        transferDirectory(localPath.toFile(), remote.toFile());
      } else {
        Files.copy(localPath, remote);
      }
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public void transferFile(String remotePath, Path localPath) throws FailedToTransferFileException {
    try {
      Path remote = Paths.get(remotePath);
      Files.createDirectories(localPath.getParent());
      if (Files.isDirectory(remote)) {
        transferDirectory(remote.toFile(), localPath.toFile());
      } else {
        Files.copy(remote, localPath);
      }
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  void transferDirectory(File src, File dest) throws IOException {
    if (src.isDirectory()) {
      if (!dest.exists()) {
        dest.mkdir();
      }
      String files[] = src.list();
      for (String file : files) {
        File srcFile = new File(src, file);
        File destFile = new File(dest, file);
        transferDirectory(srcFile, destFile);
      }
    }else{
      Files.copy(src.toPath(), dest.toPath());
    }
  }

  public static void deleteDirectory(final String dirPath) throws Exception {
    File file = new File(dirPath);
    recursiveDeleteFile(file);
  }

  private static void recursiveDeleteFile(final File file) throws Exception {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        recursiveDeleteFile(child);
      }
    }
    file.delete();
  }

  protected String toAbsoluteHomePath(String pathString) {
    if (pathString.indexOf('~') == 0) {
      pathString = pathString.replaceAll("^~", System.getProperty("user.home"));
    }
    return pathString;
  }
}
