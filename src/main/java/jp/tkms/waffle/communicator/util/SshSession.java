package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.io.IOStreamUtil;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.exception.FailedToAcquireConnectionException;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpException;

import java.io.*;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class SshSession implements AutoCloseable {
  private static final int LINE_FEED = '\n';
  static final int TIMEOUT = 15;
  static final int LONG_TIMEOUT = 180; //3min
  private static final Map<String, SessionWrapper> sessionCache = new HashMap<>();

  private static SshClient client = null;
  private SessionWrapper sessionWrapper = null;
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
    watchdogThread = new Thread(() -> {
      try (ChannelShell channel = sessionWrapper.get().createShellChannel();
           PipedOutputStream outputStream = new PipedOutputStream();
           PipedInputStream inputStream = new PipedInputStream(outputStream)) {
        channel.setOut(new OutputStream() {
          @Override
          public void write(int i) {
            updateAliveTime();
          }
        });
        channel.setIn(inputStream);
        channel.open().verify(TIMEOUT, TimeUnit.SECONDS);

        while (true) {
          long submitTime = System.currentTimeMillis();
          outputStream.write(LINE_FEED);
          Thread.sleep(TIMEOUT * 1000);
          if (previousAliveTime < submitTime) {
            if (isConnected()) {
              WarnLogMessage.issue(loggingTarget, "Connection was lost");
              disconnect();
              return;
            }
          }
        }
      } catch (IOException | InterruptedException e) {
        //NOP
      }
    });
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

    this.tunnelTargetHost = host;
    this.tunnelTargetPort = port;
  }

  public void connect(boolean retry) throws IOException, UnresolvedAddressException {
    synchronized (sessionCache) {
      sessionWrapper = sessionCache.get(getConnectionName());
      if (sessionWrapper == null) {
        sessionWrapper = new SessionWrapper();
        sessionCache.put(getConnectionName(), sessionWrapper);
      }
    }

    sessionWrapper.link(this);

    boolean connected = false;
    int waitTime = 5;
    do {
      try {
        if (!isConnected()) {
          if (tunnelSession != null) {
            tunnelSession.connect(retry);
            host = "127.0.0.1";
            port = tunnelSession.setPortForwardingL(tunnelTargetHost, tunnelTargetPort);
          }
          ClientSession session = client.connect(username, host, port).verify(TIMEOUT, TimeUnit.SECONDS).getSession();
          sessionWrapper.set(session);
          //session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, TimeUnit.SECONDS, TIMEOUT);
          session.auth().verify(TIMEOUT, TimeUnit.SECONDS);
          startWatchdog();
          connected = true;
        } else {
          connected = true;
        }
      } catch (Exception e) {
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
          WarnLogMessage.issue(loggingTarget, e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
          disconnect();
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
        disconnect();
      }
    } else {
      disconnect();
    }
  }

  public void disconnect() {
    try (ChannelProvider provider = new ChannelProvider()) {
      stopWatchdog();
      if (singleSftpClient != null) {
        try {
          if (singleSftpClient.isOpen()) {
            singleSftpClient.close();
            singleSftpClient = null;
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
          sessionCache.remove(getConnectionName());
        }
      }

      if (tunnelSession != null) {
        tunnelSession.disconnect();
      }
    } catch (Exception e) {
      //NOP
    }
  }

  public int setPortForwardingL(String hostName, int rport) throws IOException {
    tunnelTargetHost = hostName;
    tunnelTargetPort = rport;
    return sessionWrapper.get().startLocalPortForwarding(0, new SshdSocketAddress(hostName, rport)).getPort();
  }

  public ExecChannel exec(String command, String workDir) throws Exception {
    ExecChannel channel = new ExecChannel();
    boolean isFailed = false;
    do {
      isFailed = false;
      try (ChannelProvider provider = new ChannelProvider()) {
        try {
          channel.exec(command, workDir, provider);
        } catch (Exception e) {
          if (Main.hibernatingFlag) {
            throw e;
          }
          if (e instanceof NullPointerException || e instanceof SshException || e instanceof IllegalStateException || e.getMessage().equals("channel is not opened.")) {
            isFailed = true;
            provider.failed();
          } else {
            throw new RuntimeException(e);
          }
        }
      }
    } while (isFailed);
    return channel;
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
      try {
        makeDirectories(resolvedPath, sftpClient);
      } catch (IOException e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public boolean rmdir(String path, String workDir) throws Exception {
    ExecChannel channel = exec("rm -rf " + path, workDir);
    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws Exception {
    final String[] resultText = new String[1];
    final String resolvedPath = resolvePath(path, workDir);
    processSftp(sftpClient -> {
      try {
        try (InputStream inputStream = sftpClient.read(resolvedPath)) {
          resultText[0] = IOStreamUtil.readString(inputStream);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return true;
    });

    //TODO: implement an exception handling

    return resultText[0];
  }

  public synchronized boolean putText(String text, String path, String workDir) throws Exception {
    final String resolvedPath = resolvePath(path, workDir);
    return processSftp(sftpClient -> {
      try {
        try (OutputStream outputStream = sftpClient.write(resolvedPath)) {
          outputStream.write(text.getBytes());
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
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

  private String resolvePath(String path, String workDir) throws Exception {
    String resolvedPath = path;
    if (workDir != null && !"".equals(workDir)) {
      resolvedPath = Paths.get(workDir).resolve(path).normalize().toString();
    }
    resolvedPath = resolvedPath.replaceFirst("^~", getHomePath());
    return resolvedPath;
  }

  public String getHomePath() throws Exception {
    if (homePath == null) {
      homePath = exec("cd;pwd", "/").getStdout().trim();
    }
    return homePath;
  }

  public boolean scp(String remote, File local, String workDir) throws Exception {
    try {
      final String resolvedRemote = resolvePath(remote, workDir);
      boolean isDirectory = processSftp(client -> {
        try {
          return client.stat(resolvedRemote).isDirectory();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      if(isDirectory) {
        Files.createDirectories(local.toPath());
        processSftp(client -> {
          try {
            ArrayList<String> dirEntries = new ArrayList<>();
            for (SftpClient.DirEntry entry : client.readDir(resolvedRemote)) {
              if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) { continue; }
              dirEntries.add(entry.getFilename());
            }
            for (String entry : dirEntries) {
              transferFiles(resolvedRemote + "/" + entry, local.toPath(), client);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      } else {
        Files.createDirectories(local.toPath().getParent());
        processSftp(client -> {
          try {
            transferFile(resolvedRemote, local.toPath(), client);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  public boolean scp(File local, String remote, String workDir) throws Exception {
    try {
      final String resolvedRemote = resolvePath(remote, workDir);
      if (local.isDirectory()) {
        mkdir(resolvedRemote);
        processSftp(client -> {
          try {
            for(File file: local.listFiles()){
              transferFiles(file, resolvedRemote, client);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      } else {
        processSftp(client -> {
          try {
            transferFile(local, resolvedRemote, client);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      }
    } catch (Exception e) {
      //WarnLogMessage.issue(loggingTarget, e);
      //return false;
      throw new RuntimeException(e);
    }
    return processSftp(channelSftp -> {
      return true;
    });
  }

  private static boolean makeDirectories(String resolvedPath, SftpClient sftpClient) throws IOException {
    String parentPath = Paths.get(resolvedPath).getParent().toString();
    try {
      sftpClient.stat(parentPath);
    } catch (IOException e) {
      if (e.getMessage().startsWith("No such file")) {
        makeDirectories(parentPath, sftpClient);
      } else {
        throw e;
      }
    }
    try {
      sftpClient.stat(resolvedPath);
      return true;
    } catch (IOException e) {
      if (e.getMessage().startsWith("No such file")) {
        sftpClient.mkdir(resolvedPath);
      } else {
        throw e;
      }
    }
    return true;
  }

  private static void transferFiles(String remotePath, Path localPath, SftpClient sftpClient) throws IOException {
    //System.out.println(localPath + "   <<<---  " + remotePath);
    String name = Paths.get(remotePath).getFileName().toString();
    if(sftpClient.stat(remotePath).isDirectory()){
      Files.createDirectories(localPath.resolve(name));

      ArrayList<String> dirEntries = new ArrayList<>();
      boolean isFailed = false;
      do {
        isFailed = false;
        try {
          for (SftpClient.DirEntry entry : sftpClient.readDir(remotePath)) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
              continue;
            }
            dirEntries.add(entry.getFilename());
          }
        } catch (RuntimeException e) {
          isFailed = true;
        }
      } while (isFailed);
      for(String entry: dirEntries){
        Path nextPath = Paths.get(remotePath).resolve(entry);
        transferFiles(nextPath.toString(), localPath.resolve(name), sftpClient);
      }
    } else {
      transferFile(remotePath, localPath.resolve(name), sftpClient);
    }
  }

  private static void transferFile(String remotePath, Path localPath, SftpClient sftpClient) throws IOException {
    Files.copy(sftpClient.read(remotePath), localPath, StandardCopyOption.REPLACE_EXISTING);
  }

  private static void transferFiles(File localFile, String destPath, SftpClient sftpClient) throws IOException {
    //System.out.println(localFile + "   --->>>  " + destPath);
    if(localFile.isDirectory()){
      destPath = destPath + "/" + localFile.getName();
      try {
        sftpClient.mkdir(destPath);
      } catch (SftpException e) { }
      for (File file: localFile.listFiles()){
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
    SftpClient.Attributes attributes = sftpClient.stat(destPath);
    attributes.setPermissions(getPermissionOct(localFile.toPath()));
    sftpClient.setStat(destPath, attributes);
  }

  public static int getPermissionOct(Path path) {
    try {
      String perm = PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
      perm = perm.substring(perm.length() - 9);
      perm = perm.replaceAll("-", "0").replaceAll("[^0]", "1");
      return (Integer.parseInt(perm.substring(0, 3), 2) * 64)
        + (Integer.parseInt(perm.substring(3, 6), 2) * 8)
        + Integer.parseInt(perm.substring(6, 9), 2);
    } catch (IOException e) {
      return 0;
    }
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

  @Override
  public void close() throws Exception {
    disconnect();
  }

  private boolean processSftp(Function<SftpClient, Boolean> process) throws Exception {
      boolean result = false;
      boolean isFailed;
      do {
        isFailed = false;

        try (ChannelProvider provider = new ChannelProvider()) {
          try {
            result = process.apply(provider.createSftpChannel());
          } catch (Exception e) {
            if (Main.hibernatingFlag) {
              throw e;
            }
            if (e.getMessage().equals("channel is not opened.") || e.getMessage().contains("SSH_FX_FAILURE")) {
              isFailed = true;
              provider.failed();
            } else {
              throw e;
            }
          }
        }
      } while (isFailed);
      return result;
  }

  public class ExecChannel {
    private String submittedCommand;
    private String stdout;
    private String stderr;
    private int exitStatus;

    public ExecChannel() throws Exception {
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

    public ExecChannel exec(String command, String workDir, ChannelProvider provider) throws Exception {
      submittedCommand = "cd " + workDir + " && " + command;
      String fullCommand = "sh -c '" + submittedCommand.replaceAll("'", "'\\\\''") + "'";

      try (ByteArrayOutputStream stdoutOutputStream = new ByteArrayOutputStream();
           ByteArrayOutputStream stderrOutputStream = new ByteArrayOutputStream();
           ClientChannel channel = provider.createExecChannel(fullCommand, stdoutOutputStream, stderrOutputStream)) {

        int timeoutCount = 0;
        while (LONG_TIMEOUT > TIMEOUT * timeoutCount) {
          try {
            channel.waitFor(EnumSet.of(ClientChannelEvent.EXIT_STATUS), Duration.of(TIMEOUT, ChronoUnit.SECONDS));
            break;
          } catch (RuntimeException e) {
            InfoLogMessage.issue(loggingTarget, "waiting a response");
            timeoutCount += 1;
            if (LONG_TIMEOUT <= TIMEOUT * timeoutCount) {
              throw new FailedToControlRemoteException(e);
            }
          }
        }

        stdout = stdoutOutputStream.toString(StandardCharsets.UTF_8);
        stderr = stderrOutputStream.toString(StandardCharsets.UTF_8);
        exitStatus = channel.getExitStatus();
      }
      return this;
    }
  }

  SftpClient singleSftpClient = null;
  Semaphore sessionSemaphore = new Semaphore(1);

  class ChannelProvider implements AutoCloseable {
    int failedCount;
    boolean isSftpUsed;
    boolean isSessionClosed;
    ClientChannel clientChannel;

    public ChannelProvider() {
      sessionSemaphore.acquireUninterruptibly();

      failedCount = 0;
      isSftpUsed = false;
      isSessionClosed = false;
      clientChannel = null;
    }

    public ClientChannel createExecChannel(String command, OutputStream stdoutOutputStream, OutputStream stderrOutputStream) {
      boolean isFailed = false;
      do {
        isFailed = false;
        checkSession();
        try {
          clientChannel = sessionWrapper.get().createExecChannel(command);
          clientChannel.setOut(stdoutOutputStream);
          clientChannel.setErr(stderrOutputStream);
          clientChannel.open().verify(TIMEOUT, TimeUnit.SECONDS);
        } catch (IOException e) {
          isFailed = true;
          failed();
        }
      } while (isFailed);
      return clientChannel;
    }

    public SftpClient createSftpChannel() {
      isSftpUsed = true;
      boolean isFailed = false;
      do {
        isFailed = false;
        try {
          checkSession();
          if (singleSftpClient == null || singleSftpClient.isClosing() || !singleSftpClient.isOpen()) {
            singleSftpClient = SftpClientFactory.instance().createSftpClient(sessionWrapper.get());
          }
        } catch (IOException e) {
          isFailed = true;
          failed();
        }
      } while (isFailed);
      return singleSftpClient;
    }

    private void checkSession() {
      if (!isConnected()) {
        isSessionClosed = true;
        try {
          connect(true);
        } catch (IOException e) {
          throw new FailedToAcquireConnectionException(e);
        }
      }
    }

    public void failed() {
      failedCount += 1;
      if (failedCount <= 1) {
        resetChannel();
        //InfoLogMessage.issue(loggingTarget, "Retry to open channel after 1 sec.");
        Simple.sleep(TimeUnit.SECONDS, 1);
      } else {
        resetSession();
        int sleepTime = Math.min(60, failedCount);
        InfoLogMessage.issue(loggingTarget, "Reset the session and retry to open channel after " + sleepTime + " sec.");
        Simple.sleep(TimeUnit.SECONDS, sleepTime);
      }
    }

    private void resetChannel() {
      if (isSftpUsed) {
        try {
          if (singleSftpClient != null && singleSftpClient.isOpen()) {
            singleSftpClient.close();
          }
        } catch (IOException e) {
          //NOP
        }
        singleSftpClient = null;
      }

      if (clientChannel != null) {
        try {
          clientChannel.close();
        } catch (IOException e) {
          //NOP
        }
        clientChannel = null;
      }
    }

    private void resetSession() {
      resetChannel();
      disconnectSession();
    }

    private void disconnectSession() {
      sessionSemaphore.release();
      disconnect();
      sessionSemaphore.acquireUninterruptibly();
    }

    @Override
    public void close() throws Exception {
      if (clientChannel != null) {
        try {
          clientChannel.close();
        } catch (IOException e) {
          //NOP
        }
      }

      if (isSessionClosed) {
        disconnectSession();
      }

      sessionSemaphore.release();
    }
  }
}
