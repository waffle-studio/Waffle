package jp.tkms.waffle.submitter.util;

import com.jcraft.jsch.*;
import jp.tkms.waffle.data.log.WarnLogMessage;
import org.jruby.RubyProcess;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SshSession {
  private final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.ssh/config";
  private final String DEFAULT_PRIVKEY_FILE = System.getProperty("user.home") + "/.ssh/id_rsa";
  protected JSch jsch;
  protected Session session;
  Semaphore channelSemaphore = new Semaphore(4);

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

  public boolean isConnected() {
    if (session != null) {
      return session.isConnected();
    }
    return false;
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
    int waitTime = 10;
    do {
      try {
        session.connect();
        connected = true;
      } catch (JSchException e) {
        if (!retry) {
          WarnLogMessage.issue(e.getMessage());
          throw e;
        } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
          WarnLogMessage.issue(e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
          session.disconnect();
          try {
            Thread.sleep(waitTime * 1000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          waitTime += 10;
        }
      }
    } while (!connected);
  }

  public void disconnect() {
    if (channelSftp != null && channelSftp.isConnected()) {
      channelSftp.disconnect();
    }
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
          WarnLogMessage.issue("Retry to open channel after 1 sec.");
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

  public boolean mkdir(Path path) throws JSchException {
    return processSftp(channelSftp -> {
      try {
        channelSftp.stat(path.toString());
        return true;
      } catch (SftpException e) {
        if (e.getMessage().startsWith("2:")) {
          try {
            mkdir(path.getParent());
          } catch (JSchException ex) {
            WarnLogMessage.issue(e);
            return false;
          }
        }
      }
      try {
        channelSftp.mkdir(path.toString());
      } catch (SftpException e) {
        WarnLogMessage.issue(e);
        return false;
      }
      return true;
    });
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
    return processSftp(channelSftp -> {
      try {
        channelSftp.cd(workDir);
        channelSftp.put (new ByteArrayInputStream(text.getBytes ()), path);
      } catch (SftpException e) {
        WarnLogMessage.issue(e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean scp(String remote, File local, String workDir) throws JSchException {
    return processSftp(channelSftp -> {
      try {
        channelSftp.cd(workDir);
        if(channelSftp.stat(remote).isDir()) {
          Files.createDirectories(local.toPath());
          for (Object o : channelSftp.ls(remote)) {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;
            transferFiles(entry.getFilename(), local.toPath(), channelSftp);
          }
        } else {
          Files.createDirectories(local.toPath().getParent());
          transferFile(remote, local.toPath(), channelSftp);
        }
      } catch (Exception e) {
        WarnLogMessage.issue(e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean scp(File local, String dest, String workDir) throws JSchException {
    return processSftp(channelSftp -> {
      try {
        channelSftp.cd(workDir);
        if (local.isDirectory()) {
          mkdir(Paths.get(dest));
          channelSftp.cd(dest);
          for(File file: local.listFiles()){
            transferFiles(file, dest, channelSftp);
          }
        } else {
          transferFile(local, dest, channelSftp);
        }
      } catch (Exception e) {
        WarnLogMessage.issue(e);
        return false;
      }
      return true;
    });
  }

  private ChannelSftp channelSftp;
  final Object sftpLocker = new Object();
  private boolean processSftp(Function<ChannelSftp, Boolean> process) throws JSchException {
    synchronized (sftpLocker) {
      boolean result = false;
      boolean failed;
      do {
        failed = false;

        try {
          if (channelSftp == null || channelSftp.isClosed()) {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
          }

          result = process.apply(channelSftp);
        } catch (JSchException e) {
          if (e.getMessage().equals("channel is not opened.")) {
            failed = true;
            channelSftp = null;
            WarnLogMessage.issue("Retry to open channel after 1 sec.");
            try { Thread.sleep(1000); } catch (InterruptedException ex) { }
          } else {
            throw e;
          }
        }
      } while (failed);
      return result;
    }
  }

  private static void transferFiles(String remotePath, Path localPath, ChannelSftp clientChannel) throws SftpException, IOException {
    String name = Paths.get(remotePath).getFileName().toString();
    if(clientChannel.stat(remotePath).isDir()){
      Files.createDirectories(localPath.resolve(name));

      for(Object o: clientChannel.ls(remotePath)){
        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) o;
        transferFiles(entry.getFilename(), localPath.resolve(name), clientChannel);
      }
    } else {
      transferFile(remotePath, localPath.resolve(name), clientChannel);
    }
  }

  private static void transferFile(String remotePath, Path localPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
    try {
      Files.copy(clientChannel.get(remotePath), localPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void transferFiles(File localFile, String destPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
    System.out.println(localFile + "   --->>>  " + destPath);
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
