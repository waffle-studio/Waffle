package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.job.AbstractJob;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.exception.RunNotFoundException;
import jp.tkms.waffle.submitter.util.SshChannel;
import jp.tkms.waffle.submitter.util.SshSession;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class JobNumberLimitedSshSubmitter extends AbstractSubmitter {
  public static final String KEY_IDENTITY_FILE = "identity_file";
  private static final String ENCRYPTED_MARK = "#*# = ENCRYPTED = #*#";
  private static final String KEY_ENCRYPTED_IDENTITY_PASS = ".encrypted_identity_pass";

  SshSession session;
  SshSession tunnelSession;
  String home = null;

  public JobNumberLimitedSshSubmitter(Computer computer) {
    super(computer);
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

      JSONObject parameters = computer.getParametersWithDefaultParameters();
      for (Map.Entry<String, Object> entry : parameters.toMap().entrySet()) {
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
            if (entry.getValue().toString().equals(ENCRYPTED_MARK)) {
              identityPass = computer.decryptText(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS));
            } else {
              if (!entry.getValue().toString().equals("")) {
                computer.setParameter(KEY_ENCRYPTED_IDENTITY_PASS, computer.encryptText(entry.getValue().toString()));
                identityPass = entry.getValue().toString();
                computer.setParameter("identity_pass", ENCRYPTED_MARK);
              }
            }
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
        tunnelSession = new SshSession(computer);
        tunnelSession.setSession(object.getString("user"), object.getString("host"), object.getInt("port"));
        String tunnelIdentityPass = object.getString("identity_pass");
        if (tunnelIdentityPass == null) {
          tunnelIdentityPass = "";
        } else {
          if (tunnelIdentityPass.equals(ENCRYPTED_MARK)) {
            tunnelIdentityPass = computer.decryptText(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS + "_1"));
          } else {
            if (! tunnelIdentityPass.equals("")) {
              computer.setParameter(KEY_ENCRYPTED_IDENTITY_PASS + "_1", computer.encryptText(tunnelIdentityPass));
              object.put("identity_pass", ENCRYPTED_MARK);
            }
          }
        }
        if (tunnelIdentityPass.equals("")) {
          tunnelSession.addIdentity(object.getString("identity_file"));
        } else {
          tunnelSession.addIdentity(object.getString("identity_file"), tunnelIdentityPass);
        }
        tunnelSession.connect(retry);
        port = tunnelSession.setPortForwardingL(hostName, port);
        hostName = "127.0.0.1";
        computer.setParameter("tunnel", object);
      }

      session = new SshSession(computer);
      session.setSession(user, hostName, port);
      if (identityPass.equals("")) {
        session.addIdentity(identityFile);
      } else {
        session.addIdentity(identityFile, identityPass);
      }
      session.connect(retry);
    } catch (Exception e) {
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
    super.close();
    if (session != null) { session.disconnect(); }
    if (tunnelSession != null) { tunnelSession.disconnect(); }
  }

  @Override
  public Path parseHomePath(String pathString) throws FailedToControlRemoteException {
    if (pathString.indexOf('~') == 0) {
      if (home == null) {
        try {
          home = session.exec("echo -n $HOME", "~/").getStdout().replaceAll("\\r|\\n", "");
        } catch (JSchException | InterruptedException e) {
          throw new FailedToControlRemoteException(e);
        }
      }
      pathString = pathString.replaceAll("^~", home);
    }
    return Paths.get(pathString);
  }

  @Override
  public void createDirectories(Path path) throws FailedToControlRemoteException {
    try {
      session.mkdir(path);
    } catch (JSchException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public void chmod(int mod, Path path) throws FailedToControlRemoteException {
    try {
      session.chmod(mod, path);
    } catch (JSchException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String exec(String command) {
    String result = "";

    try {
      SshChannel channel = session.exec(command, "");
      result += channel.getStdout();
      result += channel.getStderr();
    } catch (JSchException | InterruptedException e) {
      //e.printStackTrace();
      return null;
    }

    return result;
  }

  @Override
  public void putText(AbstractJob job, Path path, String text) throws FailedToTransferFileException {
    try {
      session.putText(text, path.toString(), getRunDirectory(job.getRun()).toString());
    } catch (JSchException | FailedToControlRemoteException | RunNotFoundException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  boolean exists(Path path) throws FailedToControlRemoteException {
    try {
      return session.exists(path);
    } catch (JSchException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String getFileContents(ComputerTask run, Path path) {
    try {
      return session.getText(getContentsPath(run, path).toString(), "");
    } catch (JSchException | FailedToControlRemoteException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {
    try {
      session.scp(localPath.toFile(), remotePath.toString(), "/tmp");
    } catch (JSchException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {
    try {
      session.scp(remotePath.toString(), localPath.toFile(), "/tmp");
    } catch (JSchException e) {
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