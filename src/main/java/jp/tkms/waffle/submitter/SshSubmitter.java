package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.data.*;
import jp.tkms.waffle.data.exception.FailedToControlRemoteException;
import jp.tkms.waffle.data.exception.FailedToTransferFileException;
import jp.tkms.waffle.data.exception.RunNotFoundException;
import jp.tkms.waffle.submitter.util.SshChannel;
import jp.tkms.waffle.submitter.util.SshSession;
import org.jruby.RubyProcess;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SshSubmitter extends AbstractSubmitter {
  private static final String ENCRYPTED_MARK = "#*# = ENCRYPTED = #*#";
  private static final String KEY_ENCRYPTED_IDENTITY_PASS = ".encrypted_identity_pass";

  Host host;
  SshSession session;
  SshSession tunnelSession;
  String home = null;

  public SshSubmitter(Host host) {
    this.host = host;
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

      JSONObject parameters = host.getParametersWithDefaultParameters();
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
              identityPass = host.decryptText(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS));
            } else {
              host.setParameter(KEY_ENCRYPTED_IDENTITY_PASS, host.encryptText(entry.getValue().toString()));
              identityPass = entry.getValue().toString();
              host.setParameter("identity_pass", ENCRYPTED_MARK);
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
        JSONObject object = host.getParametersWithDefaultParameters().getJSONObject("tunnel");
        tunnelSession = new SshSession();
        tunnelSession.setSession(object.getString("user"), object.getString("host"), object.getInt("port"));
        String tunnelIdentityPass = object.getString("identity_pass");
        if (tunnelIdentityPass == null) {
          tunnelIdentityPass = "";
        } else {
          if (tunnelIdentityPass.equals(ENCRYPTED_MARK)) {
            tunnelIdentityPass = host.decryptText(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS + "_1"));
          } else {
            host.setParameter(KEY_ENCRYPTED_IDENTITY_PASS + "_1", host.encryptText(tunnelIdentityPass));
            object.put("identity_pass", ENCRYPTED_MARK);
          }
        }
        if (tunnelIdentityPass.equals("")) {
          tunnelSession.addIdentity(object.getString("identity_file"));
        } else {
          tunnelSession.addIdentity(object.getString("identity_file"), tunnelIdentityPass);
        }
        tunnelSession.setConfig("StrictHostKeyChecking", "no");
        tunnelSession.connect(retry);
        port = tunnelSession.setPortForwardingL(hostName, port);
        hostName = "127.0.0.1";
        host.setParameter("tunnel", object);
      }

      session = new SshSession();
      session.setSession(user, hostName, port);
      if (identityPass.equals("")) {
        session.addIdentity(identityFile);
      } else {
        session.addIdentity(identityFile, identityPass);
      }
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(retry);
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
    if (pathString.indexOf('~') == 0) {
      if (home == null) {
        try {
          home = session.exec("echo -n $HOME", "~/").getStdout().replaceAll("\\r|\\n", "");
        } catch (JSchException e) {
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
  public String exec(String command) {
    String result = "";

    try {
      SshChannel channel = session.exec(command, "");
      result += channel.getStdout();
      result += channel.getStderr();
    } catch (JSchException e) {
      e.printStackTrace();
      return null;
    }

    return result;
  }

  @Override
  public void putText(Job job, Path path, String text) throws FailedToTransferFileException {
    try {
      session.putText(text, path.toString(), getRunDirectory(job.getRun()).toString());
    } catch (JSchException | FailedToControlRemoteException | RunNotFoundException e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  boolean exists(Path path) throws FailedToControlRemoteException {
    try {
      return session.exec("test -e \"" + path.toString() + "\"", "").getExitStatus() == 0;
    } catch (JSchException e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String getFileContents(SimulatorRun run, Path path) {
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
  public JSONObject getDefaultParameters(Host host) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("host", host.getName());
    jsonObject.put("user", System.getProperty("user.name"));
    jsonObject.put("identity_file", "~/.ssh/id_rsa");
    jsonObject.put("identity_pass", "");
    jsonObject.put("port", 22);
    return jsonObject;
  }
}
