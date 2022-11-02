package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.io.IOStreamUtil;
import jp.tkms.utils.value.ObjectWrapper;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.hostbased.HostBasedAuthenticationReporter;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.keyverifier.StaticServerKeyVerifier;
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
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;


public class SshSession implements AutoCloseable {

  private static final String COMM = "COMM";
  private static final String FILE = "FILE";
  static final int TIMEOUT = 15;
  static final int LONG_TIMEOUT = 180; //3min
  private static final Map<String, SessionWrapper> sessionCache = new HashMap<>();

  static SshClient client = null;
  private SessionWrapper sessionWrapper = null;
  private SessionWrapper sftpSessionWrapper = null;
  Semaphore channelSemaphore = new Semaphore(1);
  String username;
  String host;
  int port;
  Computer loggingTarget;
  SshSession tunnelSession;
  private String tunnelTargetHost;
  private int tunnelTargetPort;
  String homePath;
  long previousAliveTime = -1;
  Thread watchdogThread = null;

  public SshSession(Computer loggingTarget, SshSession tunnelSession) {
    this.loggingTarget = loggingTarget;
    this.tunnelSession = tunnelSession;

    initialize();
  }

  static void initialize() {
    synchronized (sessionCache) {
      if (client == null) {
        client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> true);
        client.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            client.stop();
          } catch (Throwable e) {
            //nop
          }
        }));
      }
    }
  }

  public void startWatchdog() {
    stopWatchdog();
    updateAliveTime();
    watchdogThread = new Thread() {
      @Override
      public void run() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SftpClient watchdogClient = null;
        try {
          watchdogClient = SftpClientFactory.instance().createSftpClient(sessionWrapper.get());
        } catch (IOException e) {
          watchdogClient = null;
        }
        while (watchdogClient != null) {
          long submitTime = System.currentTimeMillis();
          SftpClient finalWatchdogClient = watchdogClient;
          executorService.submit(()->{
            try {
              finalWatchdogClient.stat(".");
              updateAliveTime();
            } catch (Throwable e) {}
          });
          try {
            sleep(TIMEOUT * 1000);
            if (previousAliveTime < submitTime) {
              if (isConnected(sessionWrapper)) {
                WarnLogMessage.issue(loggingTarget, "Connection was lost");
                try {
                  watchdogClient.close();
                } catch (IOException e) {
                  //nop
                } finally {
                  watchdogClient = null;
                }
                disconnect();
              }
            }
          } catch (InterruptedException e) {
            //nop
          }
        }
        executorService.shutdownNow();
      }
    };
    watchdogThread.start();
  }

  public void stopWatchdog() {
    if (watchdogThread != null) {
      watchdogThread.interrupt();
      watchdogThread = null;
    }
  }

  private void updateAliveTime() {
    previousAliveTime = System.currentTimeMillis();
  }

  public static String getSessionReport() {
    String report = "";
    for (Map.Entry<String, SessionWrapper> entry : sessionCache.entrySet()) {
      report += entry.getKey() + "[" + (entry.getValue() == null || entry.getValue().get() == null ? "null" : entry.getValue().size()) + "]\n";
    }
    return report;
  }

  public String getConnectionName(String type) {
    if (tunnelSession == null) {
      return username + "@" + host + ":" + port + (type != null ? "#" + type : "");
    } else {
      return tunnelSession.getConnectionName(null) + " -> " + username + "@" + tunnelSession.getTunnelTargetHost() + ":" + tunnelSession.getTunnelTargetPort() + (type != null ? "#" + type : "");
    }
  }

  protected String getTunnelTargetHost() {
    return tunnelTargetHost;
  }

  protected int getTunnelTargetPort() {
    return tunnelTargetPort;
  }

  public boolean isConnected(SessionWrapper sessionWrapper) {
    ClientSession session = sessionWrapper.get();
    if (session != null) {
      return session.isOpen() && session.isAuthenticated();
    }
    return false;
  }

  public boolean isConnected() {
    return isConnected(sessionWrapper) && (isConnected(sftpSessionWrapper) || sftpSessionWrapper == null);
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

    this.tunnelTargetHost = host;
    this.tunnelTargetPort = port;
  }

  public void connect(boolean retry, boolean noFile) throws IOException, UnresolvedAddressException {
    synchronized (sessionCache) {
      sessionWrapper = sessionCache.get(getConnectionName(COMM));
      if (sessionWrapper == null) {
        sessionWrapper = new SessionWrapper();
        sessionCache.put(getConnectionName(COMM), sessionWrapper);
      }
      if (!noFile) {
        sftpSessionWrapper = sessionCache.get(getConnectionName(FILE));
        if (sftpSessionWrapper == null) {
          sftpSessionWrapper = new SessionWrapper();
          sessionCache.put(getConnectionName(FILE), sftpSessionWrapper);
        }
      }
    }

    connect(retry, sessionWrapper, COMM);
    if (!noFile) {
      connect(retry, sftpSessionWrapper, FILE);
    }
  }

  public void connect(boolean retry) throws IOException, UnresolvedAddressException {
    connect(retry, false);
  }

  private void connect(boolean retry, SessionWrapper sessionWrapper, String type) throws IOException, UnresolvedAddressException {
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
          if (!isConnected(sessionWrapper)) {
            if (tunnelSession != null) {
              tunnelSession.connect(retry, true);
              host = "127.0.0.1";
              port = tunnelSession.setPortForwardingL(tunnelTargetHost, tunnelTargetPort);
            }
            ClientSession session = client.connect(username, host, port).verify(TIMEOUT, TimeUnit.SECONDS).getSession();
            sessionWrapper.set(session);
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.SECONDS, TIMEOUT);
            session.auth().verify(TIMEOUT, TimeUnit.SECONDS);

            if (COMM.equals(type) && previousAliveTime >= 0) {
              startWatchdog();
            }

            connected = true;
          } else {
            connected = true;
          }
        } catch (Exception e) {
          if (Main.hibernatingFlag) {
            disconnect(sessionWrapper, type);
            return;
          }

          if (e.getCause() instanceof UnresolvedAddressException || e.getMessage().toLowerCase().equals("userauth fail")) {
            disconnect(sessionWrapper, type);
            throw e;
          }

          if (!retry) {
            WarnLogMessage.issue(loggingTarget, e.getMessage());
            disconnect(sessionWrapper, type);
            throw e;
          } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
            WarnLogMessage.issue(loggingTarget, e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
            disconnect(sessionWrapper, type);
            Simple.sleep(TimeUnit.SECONDS, waitTime);
            if (waitTime < 60) {
              waitTime += 10;
            }
          }
        }
      } while (!connected);

      if (connected) {
        try {
          getHomePath();
        } catch (Exception e) {
          disconnect(sessionWrapper, type);
        }
      } else {
        disconnect(sessionWrapper, type);
      }
    }
  }

  public void disconnect() {
    stopWatchdog();
    disconnect(sessionWrapper, COMM);
    disconnect(sftpSessionWrapper, FILE);
  }

  private void disconnect(SessionWrapper sessionWrapper, String type) {
    synchronized (sessionWrapper) {
      if (FILE.equals(type) && sftpClient != null) {
        try {
          if (sftpClient.isOpen()) {
            sftpClient.close();
            sftpClient = null;
          }
        } catch (IOException e) {
          WarnLogMessage.issue(e);
        }
      }
      if (sessionWrapper != null && sessionWrapper.get() != null) {
        ClientSession session = sessionWrapper.get();
        if (sessionWrapper.unlink(this)) {
          try {
            if (session.isOpen()) {
              session.close();
            }
          } catch (IOException e) {
            WarnLogMessage.issue(e);
          }
          sessionCache.remove(getConnectionName(type));
        }
      }

      if (tunnelSession != null) {
        tunnelSession.disconnect();
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
        failed = false;
        try {
          channel.exec(command, workDir);
        } catch (Exception e) {
          if (Main.hibernatingFlag) {
            throw e;
          }
          if (e instanceof NullPointerException || e instanceof SshException || e instanceof IllegalStateException || e.getMessage().equals("channel is not opened.")) {
            if (count < 60) {
              count += 1;
            }
            failed = true;

            WarnLogMessage.issue(loggingTarget, "Retry to open channel after " + count + " sec.");

            if (count > 15) {
              InfoLogMessage.issue(loggingTarget, "Reset the connection of " + getConnectionName(FILE));
              disconnect(); // TODO: check
            }

            Simple.sleep(TimeUnit.SECONDS, count);

            try {
              connect(true);
            } catch (SshException ex) {
              WarnLogMessage.issue(loggingTarget, "Failed to connect");
            }
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
        OutputStream outputStream = sftpClient.write(resolvedPath);
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

  public String getHomePath() throws Exception {
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
    synchronized (sftpSessionWrapper) {
      int count = 0;
      boolean result = false;
      boolean failed;
      do {
        failed = false;

        try {
          if (sftpClient == null || sftpClient.isClosing() || !sftpClient.isOpen()) {
            sftpClient = SftpClientFactory.instance().createSftpClient(sftpSessionWrapper.get());
          }

          if (sftpClient.isClosing() || !sftpClient.isOpen()) {
            throw new IOException("SFTP client is not open");
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

            WarnLogMessage.issue(loggingTarget, "Retry to open sftp channel after " + count + " sec.");
            Simple.sleep(TimeUnit.SECONDS, count);

            try {
              connect(true, sftpSessionWrapper, FILE);
            } catch (SshException ex) {
              WarnLogMessage.issue(loggingTarget, "Failed to connect");
            }
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
        channel.open().verify(TIMEOUT, TimeUnit.SECONDS);

        if (!channel.isOpen()) {
          throw new Exception("could not acquire channel");
        }

        int timeoutCount = 0;
        while (LONG_TIMEOUT > TIMEOUT * timeoutCount && timeoutCount >= 0) {
          try {
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), Duration.of(TIMEOUT, ChronoUnit.SECONDS));
            timeoutCount = -1;
          } catch (RuntimeException e) {
            InfoLogMessage.issue(loggingTarget, "waiting a response");
            timeoutCount += 1;
            if (LONG_TIMEOUT > TIMEOUT * timeoutCount) {
              throw e;
            }
          }
        }

        stdout = stdoutOutputStream.toString(StandardCharsets.UTF_8);
        stderr = stderrOutputStream.toString(StandardCharsets.UTF_8);
        exitStatus = channel.getExitStatus();
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      } finally {
        channelSemaphore.release();
      }

      return this;
    }
  }
}
