package jp.tkms.waffle.communicator;

import jp.tkms.utils.value.ObjectWrapper;
import jp.tkms.waffle.communicator.annotation.CommunicatorDescription;
import jp.tkms.waffle.data.ComputerTask;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.computer.MasterPassword;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.util.WrappedJson;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import jp.tkms.waffle.exception.FailedToTransferFileException;
import jp.tkms.waffle.communicator.util.SshSession;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@CommunicatorDescription("SSH (limited by job number)")
public class JobNumberLimitedSshSubmitter extends AbstractSubmitter {
  public static final String KEY_IDENTITY_FILE = "identity_file";
  private static final String ENCRYPTED_MARK = "#*# = ENCRYPTED = #*#";
  private static final String KEY_ENCRYPTED_IDENTITY_PASS = ".encrypted_identity_pass";

  SshSession session;
  ObjectWrapper<String> home = new ObjectWrapper<>();

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

      WrappedJson parameters = computer.getParametersWithDefaultParameters();
      for (Map.Entry<Object, Object> entry : parameters.entrySet()) {
        switch (entry.getKey().toString()) {
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
              identityPass = MasterPassword.getDecrypted(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS, null));
            } else {
              if (!entry.getValue().toString().equals("")) {
                computer.setParameter(KEY_ENCRYPTED_IDENTITY_PASS, MasterPassword.getEncrypted(entry.getValue().toString()));
                identityPass = entry.getValue().toString();
                computer.setParameter("identity_pass", ENCRYPTED_MARK);
              }
            }
            break;
          case "port" :
            port = Integer.parseInt(entry.getValue().toString());
            break;
        }
      }

      ArrayList<WrappedJson> tunnelList = new ArrayList<>();
      WrappedJson tunnelRootObject = parameters.getObject("tunnel", null);
      {
        WrappedJson tunnelObject = tunnelRootObject;
        while (tunnelObject != null) {
          tunnelList.add(tunnelObject);
          tunnelObject = tunnelObject.getObject("tunnel", null);
        }
      }
      Collections.reverse(tunnelList);
      SshSession tunnelSession = null;
      for (WrappedJson tunnelObject : tunnelList) {
        tunnelSession = new SshSession(computer, tunnelSession);
        tunnelSession.setSession(tunnelObject.getString("user", ""),
          tunnelObject.getString("host", ""),
          tunnelObject.getInt("port", 22));
        String tunnelIdentityPass = tunnelObject.getString("identity_pass", "");
        if (tunnelIdentityPass == null) {
          tunnelIdentityPass = "";
        } else {
          if (tunnelIdentityPass.equals(ENCRYPTED_MARK)) {
            tunnelIdentityPass = MasterPassword.getDecrypted(parameters.getString(KEY_ENCRYPTED_IDENTITY_PASS + "_1", ""));
          } else {
            if (! tunnelIdentityPass.equals("")) {
              computer.setParameter(KEY_ENCRYPTED_IDENTITY_PASS + "_1", MasterPassword.getEncrypted(tunnelIdentityPass));
              tunnelObject.put("identity_pass", ENCRYPTED_MARK);
            }
          }
        }
        if (tunnelIdentityPass.equals("")) {
          tunnelSession.addIdentity(tunnelObject.getString("identity_file", ""));
        } else {
          tunnelSession.addIdentity(tunnelObject.getString("identity_file", ""), tunnelIdentityPass);
        }
      }

      if (tunnelRootObject != null) {
        computer.setParameter("tunnel", tunnelRootObject);
      }

      session = new SshSession(computer, tunnelSession);
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

    if (session != null) {
      session.disconnect(true);
    }
  }

  @Override
  public Path parseHomePath(String pathString) throws FailedToControlRemoteException {
    if (pathString.indexOf('~') == 0) {
      try {
        pathString = pathString.replaceAll("^~", home.get(() -> {
          return session.exec("echo -n $HOME", "~/").getStdout().replaceAll("\\r|\\n", "");
        }));
      } catch (Exception e) {
        throw new FailedToControlRemoteException(e);
      }
    }
    return Paths.get(pathString);
  }

  @Override
  public void createDirectories(Path path) throws FailedToControlRemoteException {
    try {
      session.mkdir(path.toString());
    } catch (Exception e) {
      throw new FailedToControlRemoteException(new Exception(e.toString()));
    }
  }

  @Override
  public void chmod(int mod, Path path) throws FailedToControlRemoteException {
    try {
      session.chmod(mod, path.toString());
    } catch (Exception e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String exec(String command) {
    String result = "";

    try {
      SshSession.ExecChannel channel = session.exec(command, "");
      result += channel.getStdout();
      result += channel.getStderr();
    } catch (Exception e) {
      WarnLogMessage.issue(computer, e);
      return null;
    }

    return result;
  }

  @Override
  boolean exists(Path path) throws FailedToControlRemoteException {
    try {
      return session.exists(path.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  boolean deleteFile(Path path) throws FailedToControlRemoteException {
    try {
      return session.rm(path.toString());
    } catch (Exception e) {
      throw new FailedToControlRemoteException(e);
    }
  }

  @Override
  public String getFileContents(ComputerTask run, Path path) {
    try {
      return session.getText(getContentsPath(run, path).toString(), "");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void transferFilesToRemote(Path localPath, Path remotePath) throws FailedToTransferFileException {
    try {
      session.scp(localPath.toFile(), remotePath.toString(), "/tmp");
    } catch (Exception e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public void transferFilesFromRemote(Path remotePath, Path localPath) throws FailedToTransferFileException {
    try {
      session.scp(remotePath.toString(), localPath.toFile(), "/tmp");
    } catch (Exception e) {
      throw new FailedToTransferFileException(e);
    }
  }

  @Override
  public WrappedJson getDefaultParameters(Computer computer) {
    WrappedJson jsonObject = new WrappedJson();
    jsonObject.put("host", computer.getName());
    jsonObject.put("user", System.getProperty("user.name"));
    jsonObject.put("identity_file", "~/.ssh/id_rsa");
    jsonObject.put("identity_pass", "");
    jsonObject.put("port", 22);
    return jsonObject;
  }
}
