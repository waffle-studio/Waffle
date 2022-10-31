package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.io.IOStreamUtil;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpException;

import java.io.*;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;


public class SshSession implements AutoCloseable {
  static final int TIMEOUT = 15;
  private static final Map<String, SessionWrapper> sessionCache = new HashMap<>();

  static SshClient client = null;
  private SessionWrapper sessionWrapper = null;
  Semaphore channelSemaphore = new Semaphore(4);
  String username;
  String host;
  int port;
  Computer loggingTarget;
  SshSession tunnelSession;
  private String tunnelTargetHost;
  private int tunnelTargetPort;
  String homePath;

  public SshSession(Computer loggingTarget, SshSession tunnelSession) {
    this.loggingTarget = loggingTarget;
    this.tunnelSession = tunnelSession;

    initialize();
  }

  static void initialize() {
    synchronized (sessionCache) {
      if (client == null) {
        client = SshClient.setUpDefaultClient();
        client.start();
      }
    }
  }

  public static String getSessionReport() {
    String report = "";
    for (Map.Entry<String, SessionWrapper> entry : sessionCache.entrySet()) {
      report += entry.getKey() + "[" + (entry.getValue() == null || entry.getValue().get() == null ? "null" : entry.getValue().size()) + "]\n";
    }
    return report;
  }

  public String getConnectionName() {
    if (tunnelSession == null) {
      return username + "@" + host + ":" + port;
    } else {
      return tunnelSession.getConnectionName() + " -> " + username + "@" + tunnelSession.getTunnelTargetHost() + ":" + tunnelSession.getTunnelTargetPort();
    }
  }

  protected String getTunnelTargetHost() {
    return tunnelTargetHost;
  }

  protected int getTunnelTargetPort() {
    return tunnelTargetPort;
  }

  public boolean isConnected() {
    ClientSession session = sessionWrapper.get();
    if (session != null) {
      return session.isOpen() && session.isAuthenticated();
    }
    return false;
  }

  public void addIdentity(String privKeyPath) throws GeneralSecurityException, IOException {
    addIdentity(privKeyPath, null);
  }

  public void addIdentity(String privKeyPath, String pass) throws GeneralSecurityException, IOException {
    FilePasswordProvider passwordProvider = (pass == null ? null : FilePasswordProvider.of(pass));
    for (KeyPair keyPair : SecurityUtils.getKeyPairResourceParser().loadKeyPairs(null, Paths.get(privKeyPath.replaceFirst("^~", System.getProperty("user.home"))), passwordProvider)) {
      client.addPublicKeyIdentity(keyPair);
    }
  }

  public void setSession(String username , String host, int port) {
    this.username = username;
    this.host = host;
    this.port = port;
  }

  public void connect(boolean retry) throws IOException, UnresolvedAddressException {
    synchronized (sessionCache) {
      sessionWrapper = sessionCache.get(getConnectionName());
      if (sessionWrapper == null) {
        sessionWrapper = new SessionWrapper();
        sessionCache.put(getConnectionName(), sessionWrapper);
      }
    }

    synchronized (sessionWrapper) {
      sessionWrapper.link(this);

      boolean connected = false;
      int waitTime = 5;
      do {
        try {
          /*
          if (sessionWrapper.getValue() != null) {
            session.disconnect();
          }
           */
          if (!isConnected()) {
            ClientSession session = client.connect(username, host, port).verify(TIMEOUT, TimeUnit.SECONDS).getSession();
            sessionWrapper.set(session);
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.SECONDS, TIMEOUT);
            session.auth().verify(TIMEOUT, TimeUnit.SECONDS);
            connected = true;
          } else {
            connected = true;
          }
        } catch (IOException e) {
          if (Main.hibernatingFlag) {
            disconnect();
            return;
          }

          if (e.getCause() instanceof UnresolvedAddressException || e.getMessage().toLowerCase().equals("userauth fail")) {
            disconnect();
            throw e;
          }

          if (!retry) {
            WarnLogMessage.issue(loggingTarget, e.getMessage());
            disconnect();
            throw e;
          } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
            ((IOException) e).printStackTrace();
            WarnLogMessage.issue(loggingTarget, e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
            disconnect();
            Simple.sleep(TimeUnit.SECONDS, waitTime);
            if (waitTime < 60) {
              waitTime += 10;
            }
          }
        }
      } while (!connected);

      if (!connected) {
        disconnect();
      }
    }
  }

  public void disconnect() {
    synchronized (sessionWrapper) {
      if (sftpClient != null) {
        try {
          if (sftpClient.isOpen()) {
            sftpClient.close();
          }
        } catch (IOException e) {
          WarnLogMessage.issue(e);
        }
      }
      if (sessionWrapper.get() != null) {
        ClientSession session = sessionWrapper.get();
        if (sessionWrapper.unlink(this)) {
          try {
            if (session.isOpen()) {
              session.close();
            }
          } catch (IOException e) {
            WarnLogMessage.issue(e);
          }
          sessionCache.remove(getConnectionName());
        }
      }

      if (tunnelSession != null) {
        tunnelSession.disconnect();
      }

      synchronized (sessionCache) {
        if (sessionCache.isEmpty()) {
          client.stop();
          client = null;
        }
      }
    }
  }

  public int setPortForwardingL(String hostName, int rport) throws IOException {
    tunnelTargetHost = hostName;
    tunnelTargetPort = rport;
    return sessionWrapper.get().startLocalPortForwarding(0, new SshdSocketAddress(hostName, rport)).getPort();
  }

  public SshChannel exec(String command, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      SshChannel channel = new SshChannel();
      int count = 0;
      boolean failed = false;
      do {
        try {
          channel.exec(command, workDir);
        } catch (Exception e) {
          if (e.getMessage().equals("channel is not opened.") && !Main.hibernatingFlag) {
            if (count < 60) {
              count += 1;
            }
            failed = true;

            WarnLogMessage.issue(loggingTarget, "Retry to open channel after " + count + " sec.");
            Simple.sleep(TimeUnit.SECONDS, count);

            connect(true);
          } else {
            throw e;
          }
        }
      } while (failed);
      return channel;
    }
  }

  public boolean chmod(int mod, String path) throws Exception {
    String finalPath = path.replaceFirst("^~", getHomePath());
    return processSftp(sftpClient -> {
      try {
        SftpClient.Attributes attributes = sftpClient.stat(finalPath);
        attributes.setPermissions(Integer.parseInt("" + mod, 8));
        sftpClient.setStat(finalPath, attributes);
      } catch (IOException e) {
        return false;
      }
      return true;
    });
  }

  public boolean exists(String path) throws Exception {
    String finalPath = path.replaceFirst("^~", getHomePath());
    if (path == null) {
      return false;
    }
    return processSftp(sftpClient -> {
      try {
        sftpClient.stat(finalPath);
      } catch (IOException e) {
        return false;
      }
      return true;
    });
  }

  public boolean mkdir(String path) throws Exception {
    String resolvedPath = path.replaceFirst("^~", getHomePath());
    return processSftp(sftpClient -> {
      String parentPath = Paths.get(resolvedPath).getParent().toString();
      try {
        sftpClient.stat(parentPath);
      } catch (IOException e) {
        if (e.getMessage().startsWith("No such file")) {
          try {
            mkdir(parentPath);
          } catch (Exception ex) {
            WarnLogMessage.issue(loggingTarget, e);
            return false;
          }
        }
        else {
          e.printStackTrace();
        }
      }
      try {
        sftpClient.stat(resolvedPath);
        return true;
      } catch (IOException e) {
        if (e.getMessage().startsWith("No such file")) {
          try {
            sftpClient.mkdir(resolvedPath);
          } catch (IOException ex) {
            WarnLogMessage.issue(loggingTarget, ex);
            return false;
          }
        }
        else {
          WarnLogMessage.issue(loggingTarget, e);
        }
      }
      return true;
    });
  }

  public boolean rmdir(String path, String workDir) throws Exception {
    SshChannel channel = exec("rm -rf " + path, workDir);

    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws IOException {
    //SshChannel channel = exec("cat " + path, workDir);

    //return channel.getStdout();
    final String[] resultText = new String[1];
    processSftp(sftpClient -> {
      try {
        String resolvedPath = path;
        if (workDir != null && !"".equals(workDir)) {
          resolvedPath = Paths.get(workDir).resolve(path).normalize().toString();
        }
        resolvedPath = resolvedPath.replaceFirst("^~", getHomePath());
        InputStream inputStream = sftpClient.read(resolvedPath);
        resultText[0] = IOStreamUtil.readString(inputStream);
        inputStream.close();
      } catch (Exception e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });

    //TODO: implement an exception handling

    return resultText[0];
  }

  public synchronized boolean putText(String text, String path, String workDir) throws IOException {
    return processSftp(sftpClient -> {
      try {
        String resolvedPath = path;
        if (workDir != null && !"".equals(workDir)) {
          resolvedPath = Paths.get(workDir).resolve(path).normalize().toString();
        }
        resolvedPath = resolvedPath.replaceFirst("^~", getHomePath());
        OutputStream outputStream = this.sftpClient.write(resolvedPath);
        outputStream.write(text.getBytes());
        outputStream.close();
      } catch (Exception e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean rm(String path) throws Exception {
    String finalPath = path.replaceFirst("^~", getHomePath());
    return processSftp(sftpClient -> {
      try {
        sftpClient.remove(finalPath);
      } catch (IOException e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public synchronized String getHomePath() throws Exception {
    if (homePath == null) {
      homePath = exec("cd;pwd", "/").getStdout().trim();
    }
    return homePath;
  }

  public synchronized boolean scp(String remote, File local, String workDir) throws IOException {
    return processSftp(sftpClient -> {
      try {
        String resolvedRemote = remote;
        if (workDir != null && !"".equals(workDir)) {
          resolvedRemote = Paths.get(workDir).resolve(remote).normalize().toString();
        }
        resolvedRemote = resolvedRemote.replaceFirst("^~", getHomePath());

        if(sftpClient.stat(resolvedRemote).isDirectory()) {
          Files.createDirectories(local.toPath());
          for (SftpClient.DirEntry entry : sftpClient.readDir(resolvedRemote)) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..") ) {
              continue;
            }
            transferFiles(resolvedRemote + "/" + entry.getFilename(), local.toPath(), sftpClient);
          }
        } else {
          Files.createDirectories(local.toPath().getParent());
          transferFile(resolvedRemote, local.toPath(), sftpClient);
        }
      } catch (Exception e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean scp(File local, String remote, String workDir) throws IOException {
    return processSftp(channelSftp -> {
      try {
        String resolvedRemote = remote;
        if (workDir != null && !"".equals(workDir)) {
          resolvedRemote = Paths.get(workDir).resolve(remote).normalize().toString();
        }
        resolvedRemote = resolvedRemote.replaceFirst("^~", getHomePath());
        if (local.isDirectory()) {
          mkdir(resolvedRemote);
          for(File file: local.listFiles()){
            transferFiles(file, resolvedRemote, channelSftp);
          }
        } else {
          transferFile(local, resolvedRemote, channelSftp);
        }
      } catch (Exception e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  private static void transferFiles(String remotePath, Path localPath, SftpClient sftpClient) throws IOException {
    //System.out.println(localPath + "   <<<---  " + remotePath);
    String name = Paths.get(remotePath).getFileName().toString();
    if(sftpClient.stat(remotePath).isDirectory()){
      Files.createDirectories(localPath.resolve(name));

      for(SftpClient.DirEntry entry: sftpClient.readDir(remotePath)){
        transferFiles(entry.getFilename(), localPath.resolve(name), sftpClient);
      }
    } else {
      transferFile(remotePath, localPath.resolve(name), sftpClient);
    }
  }

  private static void transferFile(String remotePath, Path localPath, SftpClient sftpClient) {
    try {
      Files.copy(sftpClient.read(remotePath), localPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void transferFiles(File localFile, String destPath, SftpClient sftpClient) throws IOException {
    //System.out.println(localFile + "   --->>>  " + destPath);
    if(localFile.isDirectory()){
      try {
        sftpClient.mkdir(localFile.getName());
      } catch (SftpException e) {}

      destPath = destPath + "/" + localFile.getName();

      for(File file: localFile.listFiles()){
        transferFiles(file, destPath, sftpClient);
      }
    } else {
      transferFile(localFile, destPath + "/" + localFile.getName(), sftpClient);
    }
  }

  private static void transferFile(File localFile, String destPath, SftpClient sftpClient) throws IOException {
    OutputStream outputStream = sftpClient.write(destPath, SftpClient.OpenMode.Create, SftpClient.OpenMode.Write);
    outputStream.write(Files.readAllBytes(localFile.toPath()));
    outputStream.close();
    String perm = localExec("stat '" + localFile.getAbsolutePath() + "' -c '%a'").replaceAll("\\r|\\n", "");
    SftpClient.Attributes attributes = sftpClient.stat(destPath);
    attributes.setPermissions(Integer.parseInt(perm, 8));
    sftpClient.setStat(destPath, attributes);
  }

  private static String localExec(String command) {
    String result = "";
    ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
    processBuilder.redirectErrorStream(true);

    try {
      Process process = processBuilder.start();
      return IOStreamUtil.readString(process.getInputStream());
    } catch (IOException e) {
      WarnLogMessage.issue(e);
    }

    return result;
  }

  private SftpClient sftpClient = null;
  private boolean processSftp(Function<SftpClient, Boolean> process) throws IOException {
    synchronized (sessionWrapper) {
      int count = 0;
      boolean result = false;
      boolean failed;
      do {
        failed = false;

        try {
          if (sftpClient == null || sftpClient.isClosing() || !sftpClient.isOpen()) {
            sftpClient = SftpClientFactory.instance().createSftpClient(sessionWrapper.get());
          }

          result = process.apply(sftpClient);
        } catch (IOException e) {
          if (e.getMessage().equals("channel is not opened.") && !Main.hibernatingFlag) {
            if (count < 60) {
              count += 1;
            }
            failed = true;
            if (sftpClient != null && sftpClient.isOpen()) {
              sftpClient.close();
            }
            sftpClient = null;

            WarnLogMessage.issue(loggingTarget, "Retry to open channel after " + count + " sec.");
            Simple.sleep(TimeUnit.SECONDS, count);

            connect(true);
          } else {
            throw e;
          }
        }
      } while (failed);
      return result;
    }
  }

  @Override
  public void close() throws Exception {
    disconnect();
  }

  public class SshChannel {
    private String submittedCommand;
    private String stdout;
    private String stderr;
    private int exitStatus;

    public SshChannel() throws Exception {
      stdout = "";
      stderr = "";
      exitStatus = -1;
    }

    public String getStdout() {
      return stdout;
    }

    public String getStderr() {
      return stderr;
    }

    public int getExitStatus() {
      return exitStatus;
    }

    public String getSubmittedCommand() {
      return submittedCommand;
    }

    public SshChannel exec(String command, String workDir) throws Exception {
      submittedCommand = "cd " + workDir + " && " + command;
      String fullCommand = "sh -c '" + submittedCommand.replaceAll("'", "'\\\\''") + "'";

      channelSemaphore.acquire();
      try (ClientChannel channel = sessionWrapper.get().createExecChannel(fullCommand);
           ByteArrayOutputStream stdoutOutputStream = new ByteArrayOutputStream();
           ByteArrayOutputStream stderrOutputStream = new ByteArrayOutputStream();) {
        channel.setOut(stdoutOutputStream);
        channel.setErr(stderrOutputStream);
        channel.open().verify(TIMEOUT, TimeUnit.MINUTES);

        channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), Duration.of(TIMEOUT, ChronoUnit.MINUTES));
        stdout = stdoutOutputStream.toString(StandardCharsets.UTF_8);
        stderr = stderrOutputStream.toString(StandardCharsets.UTF_8);
        exitStatus = channel.getExitStatus();
      } finally {
        channelSemaphore.release();
      }

      return this;
    }
  }
}
