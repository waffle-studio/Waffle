package jp.tkms.waffle.submitter.util;

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class SshSession {
  private final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.ssh/config";
  private final String DEFAULT_PRIVKEY_FILE = System.getProperty("user.home") + "/.ssh/id_rsa";
  protected JSch jsch;
  protected Session session;
  Semaphore channelSemaphore = new Semaphore(5);

  public SshSession() throws JSchException {
    this.jsch = new JSch();
    if (Files.exists(Paths.get(DEFAULT_CONFIG_FILE))) {
      try {
        jsch.setConfigRepository(OpenSSHConfig.parseFile(DEFAULT_CONFIG_FILE));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (Files.exists(Paths.get(DEFAULT_PRIVKEY_FILE))) {
      jsch.addIdentity(DEFAULT_PRIVKEY_FILE);
    }
  }

  public void addIdentity(String privKey) throws JSchException {
    jsch.addIdentity(privKey);
  }

  public void addIdentity(String privKey, String pass) throws JSchException {
    jsch.addIdentity(privKey, pass);
  }

  public void setSession(Session session) {
    this.session = session;
  }

  public void setSession(String username , String host, int port) throws JSchException {
    setSession(jsch.getSession(username, host, port));
  }

  public void setConfig(String key, String value) {
    session.setConfig(key, value);
  }

  public void connect(boolean retry) throws JSchException {
    boolean connected = false;
    do {
      try {
        session.connect();
        connected = true;
      } catch (JSchException e) {
        System.err.println(e.getMessage());
        if (!retry) {
          throw e;
        } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
          int waitTime = 10 + (int) (Math.random() * 10);
          System.err.println("Retry connection after " + waitTime + " sec.");
          try {
            Thread.sleep(waitTime * 1000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
        }
      }
    } while (!connected);
  }

  public void disconnect() {
    session.disconnect();
  }

  protected Channel openChannel(String type) throws JSchException {
    try {
      channelSemaphore.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return session.openChannel(type);
  }

  public int setPortForwardingL(String hostName, int rport) throws JSchException {
    return session.setPortForwardingL(0, hostName, rport);
  }

  public SshChannel exec(String command, String workDir) throws JSchException {
    SshChannel channel = new SshChannel((ChannelExec) openChannel("exec"));
    boolean failed = false;
    do {
      try {
        channel.exec(command, workDir);
      } catch (JSchException e) {
        if (e.getMessage().equals("channel is not opened.")) {
          failed = true;
          channelSemaphore.release();
          System.err.println("Retry to open channel after 1 sec.");
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          channel = new SshChannel((ChannelExec) openChannel("exec"));
        } else {
          throw e;
        }
      }
    } while (failed);
    channelSemaphore.release();
    return channel;
  }

  public boolean mkdir(String path, String workDir) throws JSchException {
    SshChannel channel = exec("mkdir -p " + path, workDir);

    return (channel.getExitStatus() == 0);
  }

  public boolean rmdir(String path, String workDir) throws JSchException {
    SshChannel channel = exec("rm -rf " + path, workDir);

    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws JSchException {
    SshChannel channel = exec("cat " + path, workDir);

    return channel.getStdout();
  }

  public boolean putText(String text, String path, String workDir) throws JSchException {
    ChannelSftp channelSftp = (ChannelSftp) openChannel("sftp");
    boolean result = false;
    do {
      try {
        channelSftp.connect();
        try {
          channelSftp.cd(workDir);
          channelSftp.put (new ByteArrayInputStream(text.getBytes ()), path);
          result = true;
        } catch (SftpException e) {
          e.printStackTrace();
        }finally {
          channelSftp.disconnect();
          channelSemaphore.release();
        }
      } catch (JSchException e) {
        if (e.getMessage().equals("channel is not opened.")) {
          channelSemaphore.release();
          System.err.println("Retry to open channel after 1 sec.");
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          channelSftp = (ChannelSftp) openChannel("sftp");
        } else {
          throw e;
        }
      }
    } while (!result);
    return result;
  }

  public boolean scp(File local, String dest, String workDir) throws JSchException {
    ChannelSftp channelSftp = (ChannelSftp) openChannel("sftp");
    boolean result = false;
    do {
      try {
        channelSftp.connect();
        try {
          channelSftp.cd(workDir);
          try {
            channelSftp.mkdir(dest);
          } catch (SftpException e) {}
          channelSftp.cd(dest);
          for(File file: local.listFiles()){
            transferFiles(file, dest, channelSftp);
          }
          result = true;
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          channelSftp.disconnect();
          channelSemaphore.release();
        }
      } catch (JSchException e) {
        if (e.getMessage().equals("channel is not opened.")) {
          channelSemaphore.release();
          System.err.println("Retry to open channel after 1 sec.");
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          channelSftp = (ChannelSftp) openChannel("sftp");
        } else {
          throw e;
        }
      }
    } while (!result);
    return result;
  }

  private static void transferFiles(File localFile, String destPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
    if(localFile.isDirectory()){
      try {
        clientChannel.mkdir(localFile.getName());
      } catch (SftpException e) {}

      destPath = destPath + "/" + localFile.getName();
      clientChannel.cd(destPath);

      for(File file: localFile.listFiles()){
        transferFiles(file, destPath, clientChannel);
      }
      clientChannel.cd("..");
    } else {
      transferFile(localFile, localFile.getName(), clientChannel);
    }
  }

  private static void transferFile(File localFile, String destPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
    clientChannel.put(localFile.getAbsolutePath(), destPath, ChannelSftp.OVERWRITE);

    String perm = localExec("stat '" + localFile.getAbsolutePath() + "' -c '%a'").replaceAll("\\r|\\n", "");
    clientChannel.chmod(Integer.parseInt(perm, 8), destPath);
  }

  private static String localExec(String command) {
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
}
