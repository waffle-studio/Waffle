package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.log.InfoLogMessage;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalSubmitter extends AbstractSubmitter {
  @Override
  public AbstractSubmitter connect(boolean retry) {
    return this;
  }

  @Override
  public String getRunDirectory(SimulatorRun run) {
    Host host = run.getHost();
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
  String getSimulatorBinDirectory(SimulatorRun run) {
    //String pathString = host.getWorkBaseDirectory() + sep + SIMULATOR_DIR + sep+ run.getSimulator().getId() + sep + Simulator.KEY_REMOTE;
    String pathString = run.getHost().getWorkBaseDirectory() + File.separator + SIMULATOR_DIR + File.separator + run.getSimulator().getVersionId();

    return toAbsoluteHomePath(pathString);
  }

  @Override
  void prepareSubmission(SimulatorRun run) {
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
    InfoLogMessage.issue("Run(" + run.getShortId() + ") was prepared");
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
  void postProcess(SimulatorRun run) {
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
  public void putText(SimulatorRun run, String path, String text) {
    try {
      PrintWriter pw = new PrintWriter(new BufferedWriter(
        new FileWriter(getRunDirectory(run) + File.separator + path)
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
  public JSONObject defaultParameters(Host host) {
    return new JSONObject();
  }

  @Override
  public void transferFile(Path localPath, String remotePath) {
    try {
      Path remote = Paths.get(remotePath);
      Files.createDirectories(remote.getParent());
      if (Files.isDirectory(localPath)) {
        transferDirectory(localPath.toFile(), remote.toFile());
      } else {
        Files.copy(localPath, remote);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void transferFile(String remotePath, Path localPath) {
    try {
      Path remote = Paths.get(remotePath);
      Files.createDirectories(localPath.getParent());
      if (Files.isDirectory(remote)) {
        transferDirectory(remote.toFile(), localPath.toFile());
      } else {
        Files.copy(remote, localPath);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void transferDirectory(File src, File dest) {
    try {
      if (src.isDirectory()) {
        if (!dest.exists()) {
          dest.mkdir();
        }
        String files[]= src.list();
        for (String file : files) {
          File srcFile = new File(src, file);
          File destFile = new File(dest, file);
          transferDirectory(srcFile, destFile);
        }
      }else{
        Files.copy(src.toPath(), dest.toPath());
      }
    } catch (Exception e) {}
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
