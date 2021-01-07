package jp.tkms.waffle.submitter;

import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.Job;
import jp.tkms.waffle.data.project.workspace.run.SimulatorRun;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.submitter.util.SshChannel2;
import jp.tkms.waffle.submitter.util.SshSession2;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SshSubmitter2 extends AbstractSubmitter {

  Computer computer;
  SshSession2 session;
  SshSession2 tunnelSession;

  public SshSubmitter2(Computer computer) {
    this.computer = computer;
  }

  @Override
  public AbstractSubmitter connect(boolean retry) {
    try {
      String hostName = "";
      String user = "";
      String identityFile = "";
      String identityPass = "";
      int port = 22;
      boolean useTunnel = false;

      for (Map.Entry<String, Object> entry : computer.getParametersWithDefaultParameters().toMap().entrySet()) {
        switch (entry.getKey()) {
          case "host" :
            hostName = entry.getValue().toString();
            break;
          case "user" :
            user = entry.getValue().toString();
            break;
          case "identity_file" :
            identityFile = entry.getValue().toString();
            break;
          case "identity_pass" :
            identityPass = entry.getValue().toString();
            break;
          case "port" :
            port = Integer.parseInt(entry.getValue().toString());
            break;
          case "tunnel" :
            useTunnel = true;
            break;
        }
      }

      if (useTunnel) {
        JSONObject object = computer.getParametersWithDefaultParameters().getJSONObject("tunnel");
        tunnelSession = new SshSession2();
        tunnelSession.setSession(object.getString("user"), object.getString("host"), object.getInt("port"));
        if (object.getString("identity_pass").equals("")) {
          tunnelSession.addIdentity(object.getString("identity_file"));
        } else {
          tunnelSession.addIdentity(object.getString("identity_file"), object.getString("identity_pass"));
        }
      }

      session = new SshSession2();
      session.setSession(user, hostName, port);
      if (identityPass.equals("")) {
        session.addIdentity(identityFile);
      } else {
        session.addIdentity(identityFile, identityPass);
      }
      if (tunnelSession == null) {
        session.connect(retry);
      } else {
        session.connect(retry, tunnelSession.getSshClientAfterConnect());
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }

    return this;
  }

  @Override
  public boolean isConnected() {
    if (session != null) {
      return session.isConnected();
    }
    return false;
  }

  @Override
  public void close() {
    session.disconnect();
    if (tunnelSession != null) { tunnelSession.disconnect(); }
  }

  @Override
  public Path parseHomePath(String pathString) throws FailedToControlRemoteException {
    try {
      if (pathString.indexOf('~') == 0) {
        pathString = pathString.replaceAll("^~", session.exec("echo $HOME", "~/").getStdout().replaceAll("\\r|\\n", ""));
      }
    } catch (IOException e) {
      throw new FailedToControlRemoteException(e);
    }
    return Paths.get(pathString);
  }

  @Override
  public void createDirectories(Path path) throws FailedToControlRemoteException {
    try {
      session.mkdir(path);
    } catch (IOException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String exec(String command) {
    String result = "";

    try {
      SshChannel2 channel = session.exec(command, "");
      result += channel.getStdout();
      result += channel.getStderr();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return result;
  }

  @Override
  public void putText(Job job, Path path, String text) throws FailedToTransferFileException {
    try {
      session.putText(text, path.toString(), getRunDirectory(job.getRun()).toString());
    } catch (IOException | FailedToControlRemoteException | RunNotFoundException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  boolean exists(Path path) throws FailedToControlRemoteException {
    try {
      return session.exec("test -e \"" + path.toString() + "\"", "").toCommand().getExitStatus() == 0;
    } catch (IOException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String getFileContents(SimulatorRun run, Path path) {
    try {
      return session.getText(getContentsPath(run, path).toString(), "");
    } catch (IOException | FailedToControlRemoteException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {
    try {
      session.scp(localPath.toFile(), remotePath.toString());
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {
    try {
      session.scp(remotePath.toString(), localPath.toFile());
    } catch (IOException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public JSONObject getDefaultParameters(Computer computer) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("host", computer.getName());
    jsonObject.put("user", System.getProperty("user.name"));
    jsonObject.put("identity_file", "~/.ssh/id_rsa");
    jsonObject.put("identity_pass", "");
    jsonObject.put("port", 22);
    return jsonObject;
  }
}
