package jp.tkms.waffle.communicator;

import jp.tkms.waffle.communicator.annotation.CommunicatorDescription;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@CommunicatorDescription("Local (limited by job number)")
public class JobNumberLimitedLocalSubmitter extends AbstractSubmitter {

  public JobNumberLimitedLocalSubmitter(Computer computer) {
    super(computer);
  }

  @Override
  public AbstractSubmitter connect(boolean retry) {
    return this;
  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public Path parseHomePath(String pathString) throws FailedToControlRemoteException {
    if (pathString.startsWith("~")) {
      pathString = pathString.replaceAll("^~", System.getProperty("user.home"));
    } else if (!pathString.startsWith("/")) {
      pathString = Paths.get(pathString).toAbsolutePath().toString();
    }
    return Paths.get(pathString);
  }

  @Override
  public void createDirectories(Path path) throws FailedToControlRemoteException {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new FailedToControlRemoteException(e);
    }
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
      ErrorLogMessage.issue(e);
    }

    return result;
  }

  @Override
  boolean exists(Path path) {
    return Files.exists(path);
  }

  @Override
  boolean deleteFile(Path path) throws FailedToControlRemoteException {
    try {
      Files.delete(path);
      return true;
    } catch (IOException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String getFileContents(ComputerTask run, Path path) throws FailedToTransferFileException {
    String result = null;
    try {
      result = exec("cat " + getContentsPath(run, path));
    } catch (FailedToControlRemoteException e) {
      throw new FailedToTransferFileException(e);
    }
    return result;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {
    try {
      Files.createDirectories(remotePath.getParent());
      if (Files.isDirectory(localPath)) {
        transferDirectory(localPath.toFile(), remotePath.toFile());
      } else {
        Files.copy(localPath, remotePath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath, Boolean isDir) throws FailedToTransferFileException {
    try {
      Files.createDirectories(localPath.getParent());
      if (Files.isDirectory(remotePath)) {
        transferDirectory(remotePath.toFile(), localPath.toFile());
      } else {
        Files.copy(remotePath, localPath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public WrappedJson getDefaultParameters(Computer computer) {
    return new WrappedJson();
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
      Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
}
