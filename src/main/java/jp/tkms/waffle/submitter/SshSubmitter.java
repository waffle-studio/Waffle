package jp.tkms.waffle.submitter;

import com.jcraft.jsch.JSchException;
import jp.tkms.waffle.data.BrowserMessage;
import jp.tkms.waffle.data.Host;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.data.Simulator;
import jp.tkms.waffle.data.log.InfoLogMessage;
import jp.tkms.waffle.submitter.util.SshChannel;
import jp.tkms.waffle.submitter.util.SshSession;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Map;

public class SshSubmitter extends AbstractSubmitter {

  Host host;
  SshSession session;
  SshSession tunnelSession;

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

      for (Map.Entry<String, Object> entry : host.getParametersWithoutXsubParameter().toMap().entrySet()) {
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
        JSONObject object = host.getParametersWithoutXsubParameter().getJSONObject("tunnel");
        tunnelSession = new SshSession();
        tunnelSession.setSession(object.getString("user"), object.getString("host"), object.getInt("port"));
        if (identityPass.equals("")) {
          tunnelSession.addIdentity(object.getString("identity_file"));
        } else {
          tunnelSession.addIdentity(object.getString("identity_file"), object.getString("identity_pass"));
        }
        tunnelSession.setConfig("StrictHostKeyChecking", "no");
        tunnelSession.connect(retry);
        port = tunnelSession.setPortForwardingL(hostName, port);
        hostName = "127.0.0.1";
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
  String getRunDirectory(SimulatorRun run) {
    Host host = run.getHost();
    String pathString = host.getWorkBaseDirectory() + host.getDirectorySeparetor()
      + RUN_DIR + host.getDirectorySeparetor() + run.getId();

    try {
      session.mkdir(pathString, "~/");
    } catch (JSchException e) {
      e.printStackTrace();
    }

    return toAbsoluteHomePath(pathString);
  }

  @Override
  String getWorkDirectory(SimulatorRun run) {
    return getRunDirectory(run) + "/" + SimulatorRun.WORKING_DIR; // do refactor
  }

  @Override
  String getSimulatorBinDirectory(SimulatorRun run) {
    String sep = run.getHost().getDirectorySeparetor();
    String pathString = host.getWorkBaseDirectory() + sep + SIMULATOR_DIR + sep+ run.getSimulator().getId() + sep + Simulator.KEY_REMOTE;

    return toAbsoluteHomePath(pathString);
  }

  @Override
  void prepareSubmission(SimulatorRun run) {
    try {
      session.mkdir(getSimulatorBinDirectory(run), "/tmp");
      if (true) {  // TODO: Check if the simulator has been updated
        session.scp(run.getSimulator().getBinDirectory().toFile(), getSimulatorBinDirectory(run), "/tmp");
        session.scp(run.getWorkPath().toFile(), getRunDirectory(run), "/tmp");
      }
    } catch (JSchException e) {
      e.printStackTrace();
    }
    InfoLogMessage.issue("Run(" + run.getShortId() + ") was prepared");
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
  int getExitStatus(SimulatorRun run) throws Exception {
    int status = -1;

    status = Integer.valueOf(session.getText(EXIT_STATUS_FILE, getRunDirectory(run)).replaceAll("\\r|\\n", ""));

    return status;
  }

  @Override
  void postProcess(SimulatorRun run) {
    try {
      //session.rmdir(getRunDirectory(run), "/tmp");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {
    session.disconnect();
    if (tunnelSession != null) { tunnelSession.disconnect(); }
  }

  @Override
  public void putText(SimulatorRun run, String path, String text) {
    try {
      session.putText(text, path, getRunDirectory(run));
    } catch (JSchException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getFileContents(SimulatorRun run, String path) {
    try {
      return session.getText(getContentsPath(run, path), "");
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return null;
  }

  public JSONObject defaultParameters(Host host) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("host", host.getName());
    jsonObject.put("user", System.getProperty("user.name"));
    jsonObject.put("identity_file", "~/.ssh/id_rsa");
    jsonObject.put("identity_pass", "");
    jsonObject.put("port", 22);
    return jsonObject;
  }

  @Override
  void transferFile(Path localPath, String remotePath) {
    try {
      session.scp(localPath.toFile(), remotePath, "/tmp");
    } catch (JSchException e) {
      e.printStackTrace();
    }
  }

  @Override
  void transferFile(String remotePath, Path localPath) {
    try {
      session.scp(remotePath, localPath.toFile(), "/tmp");
    } catch (JSchException e) {
      e.printStackTrace();
    }
  }

  protected String toAbsoluteHomePath(String pathString) {
    try {
      if (pathString.indexOf('~') == 0) {
        pathString = pathString.replaceAll("^~", session.exec("echo $HOME", "~/").getStdout().replaceAll("\\r|\\n", ""));
      }
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return pathString;
  }
}
