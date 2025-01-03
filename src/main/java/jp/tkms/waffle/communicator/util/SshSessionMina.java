package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.debug.DebugElapsedTime;
import jp.tkms.utils.io.IOStreamUtil;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.exception.FailedToAcquireConnectionException;
import jp.tkms.waffle.exception.FailedToControlRemoteException;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.session.helpers.AbstractConnectionService;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpException;

import java.io.*;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class SshSessionMina implements AutoCloseable {

  private static final boolean MODE_SFTP = false;
  private static final int LINE_FEED = '\n';
  static final int TIMEOUT = 60;
  static final int LONG_TIMEOUT = 300; //5min
  private static final Map<String, SessionWrapperMina> sessionCache = new HashMap<>();

  private static SshClient client = null;
  private SessionWrapperMina sessionWrapper = null;
  String username;
  String host;
  int port;
  Computer loggingTarget;
  SshSessionMina tunnelSession;
  private String tunnelTargetHost;
  private int tunnelTargetPort;
  String homePath;
  long previousAliveTime = -1;
  Thread watchdogThread = null;

  public SshSessionMina(Computer loggingTarget, SshSessionMina tunnelSession) {
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

  private void startWatchdog() {
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
              disconnect(false);
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

  private void stopWatchdog() {
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
    for (Map.Entry<String, SessionWrapperMina> entry : sessionCache.entrySet()) {
      report += entry.getKey() + "[" + (entry.getValue() == null || entry.getValue().get() == null ? "null" : entry.getValue().size()) + "]\n";
    }
    return report;
  }

  private String getConnectionName() {
    if (tunnelSession == null) {
      return username + "@" + host + ":" + port;
    } else {
      return tunnelSession.getConnectionName() + " -> " + username + "@" + tunnelTargetHost + ":" + tunnelTargetPort;
    }
  }

  public void addIdentity(String privKeyPath) throws GeneralSecurityException, IOException {
    synchronized (sessionCache) {
      addIdentity(privKeyPath, null);
    }
  }

  public void addIdentity(String privKeyPath, String pass) throws GeneralSecurityException, IOException {
    synchronized (sessionCache) {
      FilePasswordProvider passwordProvider = (pass == null ? null : FilePasswordProvider.of(pass));
      for (KeyPair keyPair : SecurityUtils.getKeyPairResourceParser().loadKeyPairs(null, Paths.get(privKeyPath.replaceFirst("^~", System.getProperty("user.home"))), passwordProvider)) {
        client.addPublicKeyIdentity(keyPair);
      }
    }
  }

  public void setSession(String username , String host, int port) {
    synchronized (sessionCache) {
      this.username = username;
      this.host = host;
      this.port = port;

      this.tunnelTargetHost = host;
      this.tunnelTargetPort = port;
    }
  }

  public boolean isConnected() {
    if (sessionWrapper == null) {
      return false;
    }

    synchronized (sessionWrapper) {
      ClientSession session = sessionWrapper.get();
      if (session != null) {
        return session.isOpen() && session.isAuthenticated();
      }
      return false;
    }
  }

  public void connect(boolean retry) throws IOException, UnresolvedAddressException {
    synchronized (sessionCache) {
      sessionWrapper = sessionCache.get(getConnectionName());
      if (sessionWrapper == null) {
        sessionWrapper = new SessionWrapperMina();
        sessionCache.put(getConnectionName(), sessionWrapper);
      }
    }

    synchronized (sessionWrapper) {
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
            //session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, Duration.ofSeconds(2));
            session.auth().verify(TIMEOUT, TimeUnit.SECONDS);
            startWatchdog();
            connected = true;
          } else {
            connected = true;
          }
        } catch (Exception e) {
          if (Main.hibernatingFlag) {
            disconnect(false);
            return;
          }

          if (e.getCause() instanceof UnresolvedAddressException || e.getMessage().toLowerCase().equals("userauth fail")) {
            disconnect(false);
            throw e;
          }

          if (!retry) {
            WarnLogMessage.issue(loggingTarget, e.getMessage());
            disconnect(false);
            throw e;
          } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
            WarnLogMessage.issue(loggingTarget, e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
            disconnect(false);
            Simple.sleep(TimeUnit.SECONDS, waitTime);
            if (waitTime < 60) {
              waitTime += 10;
            }
          }
        }

        if (!connected) {
          disconnect(false);
        }
      } while (!connected);
    }
  }

  public void disconnect(boolean isNormal) {
    if (sessionWrapper != null) {
      synchronized (sessionWrapper) {
        stopWatchdog();
        if (sessionWrapper != null && sessionWrapper.get() != null) {
          ClientSession session = sessionWrapper.get();
          if (sessionWrapper.unlink(this) || !isNormal) {
            if (sessionWrapper.sftpClient != null) {
              try {
                sessionWrapper.sftpClient.close();
                sessionWrapper.sftpClient.getChannel().close(false).awaitUninterruptibly();
                while (!sessionWrapper.sftpClient.getChannel().isClosed()) {
                  Simple.sleep(TimeUnit.MILLISECONDS, 10);
                }
              } catch (IOException e) {
                //NOP
              }
              sessionWrapper.sftpClient = null;
            }
            synchronized (sessionCache) {
              session.close(false).awaitUninterruptibly();
              while (!session.isClosed()) {
                Simple.sleep(TimeUnit.MILLISECONDS, 10);
              }
              try {
                session.disconnect(75, "");
              } catch (IOException e) {
                //NOP
              }
              synchronized (sessionCache) {
                sessionCache.remove(getConnectionName());
              }
            }
          }
        }

        if (tunnelSession != null) {
          tunnelSession.disconnect(isNormal);
        }
      }
    }
  }

  public int setPortForwardingL(String hostName, int rport) throws IOException {
    synchronized (sessionWrapper) {
      tunnelTargetHost = hostName;
      tunnelTargetPort = rport;
      return sessionWrapper.get().startLocalPortForwarding(0, new SshdSocketAddress(hostName, rport)).getPort();
    }
  }

  public ExecChannel exec(String command, String workDir) throws Exception {
    synchronized (sessionWrapper) {
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
          provider.close();
        }
      } while (isFailed);
      return channel;
    }
  }

  private static String commandString(String... words) {
    StringBuilder builder = new StringBuilder();
    for (String w : words) {
      builder.append("\"");
      builder.append(w.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\""));
      builder.append("\" ");
    }
    return builder.toString();
  }

  public boolean chmod(int mod, String path) throws Exception {
    synchronized (sessionWrapper) {
      String finalPath = path.replaceFirst("^~", getHomePath());
      if (MODE_SFTP) {
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
      } else {
        return exec(commandString("chmod", String.valueOf(mod), finalPath), "/tmp").getExitStatus() == 0;
      }
    }
  }

  public boolean exists(String path) throws Exception {
    synchronized (sessionWrapper) {
      String finalPath = path.replaceFirst("^~", getHomePath());
      if (path == null) {
        return false;
      }
      if (MODE_SFTP) {
        return processSftp(sftpClient -> {
          try {
            sftpClient.stat(finalPath);
          } catch (IOException e) {
            if (e.getMessage().contains("No such file")) {
              return false;
            }
            throw new RuntimeException(e);
          }
          return true;
        });
      } else {
        return exec(commandString("test", "-e", path), "/tmp").getExitStatus() == 0;
      }
    }
  }

  public boolean mkdir(String path) throws Exception {
    synchronized (sessionWrapper) {
      String resolvedPath = path.replaceFirst("^~", getHomePath());
      if (MODE_SFTP) {
        return processSftp(sftpClient -> {
          try {
            makeDirectories(resolvedPath, sftpClient);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      } else {
        return exec(commandString("mkdir", "-p", resolvedPath), "/tmp").getExitStatus() == 0;
      }
    }
  }

  public boolean rmdir(String path, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      return exec("rm -rf " + path, workDir).getExitStatus() == 0;
    }
  }

  public String getText(String path, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      final String[] resultText = new String[1];
      final String resolvedPath = resolvePath(path, workDir);
      if (MODE_SFTP) {
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
      } else {
        processScp(scpClient -> {
          try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            scpClient.download(resolvedPath, outputStream);
            resultText[0] = outputStream.toString();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      }

      //TODO: implement an exception handling

      return resultText[0];
    }
  }

  public boolean putText(String text, String path, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      final String resolvedPath = resolvePath(path, workDir);
      if (MODE_SFTP) {
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
      } else {
        byte[] data = text.getBytes();
        long currentTime = System.currentTimeMillis();
        return processScp(scpClient -> {
          try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            scpClient.upload(inputStream, resolvedPath, data.length, PosixFilePermissions.fromString("rw-rw----"), new ScpTimestampCommandDetails(currentTime, currentTime));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        });
      }
    }
  }

  public boolean rm(String path) throws Exception {
    synchronized (sessionWrapper) {
      String finalPath = path.replaceFirst("^~", getHomePath());
      if (MODE_SFTP) {
        return processSftp(sftpClient -> {
          try {
            sftpClient.remove(finalPath);
          } catch (IOException e) {
            if (e.getMessage().contains("No such file")) {
              return false;
            }
            throw new RuntimeException(e);
          }
          return true;
        });
      } else {
        return exec(commandString("rm", finalPath), "/tmp").getExitStatus() == 0;
      }
    }
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
    if (homePath == null || homePath.equals("")) {
      synchronized (sessionWrapper) {
        if (homePath == null || homePath.equals("")) {
          ExecChannel channel = new ExecChannel();
          boolean isFailed = false;
          do {
            isFailed = false;
            try (ChannelProvider provider = new ChannelProvider()) {
              try {
                channel.exec("cd;pwd", "/", provider);
                if (channel.getExitStatus() != 0 || channel.getStdout().isEmpty()) {
                  throw new NullPointerException("Failed to get home path");
                }
                homePath = channel.getStdout().trim();
              } catch (Exception e) {
                if (Main.hibernatingFlag) {
                  throw e;
                }
                if (e instanceof NullPointerException || e instanceof SshException
                  || e instanceof IllegalStateException || e.getMessage().equals("channel is not opened.")) {
                  isFailed = true;
                  provider.failed();
                } else {
                  throw new RuntimeException(e);
                }
              }
              provider.close();
            }
          } while (isFailed);
        }
      }
    }
    return homePath;
  }

  public boolean isDirectory(String remote) throws Exception {
    if (MODE_SFTP) {
      return processSftp(client -> {
        try {
          return client.stat(remote).isDirectory();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } else {
      return exec(commandString("test", "-d", remote), "/tmp").getExitStatus() == 0;
    }
  }

  public boolean scp(String remote, File local, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      try {
        final String resolvedRemote = resolvePath(remote, workDir);
        boolean isDirectory = isDirectory(resolvedRemote);

        if (isDirectory) {
          Files.createDirectories(local.toPath());
          if (MODE_SFTP) {
            processSftp(client -> {
              try {
                ArrayList<String> dirEntries = new ArrayList<>();
                for (SftpClient.DirEntry entry : client.readDir(resolvedRemote)) {
                  if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                    continue;
                  }
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
            return processScp(scpClient -> {
              try {
                scpClient.download(resolvedRemote, local.toPath(), ScpClient.Option.Recursive, ScpClient.Option.TargetIsDirectory);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          }
        } else {
          Files.createDirectories(local.toPath().getParent());
          if (MODE_SFTP) {
            processSftp(client -> {
              try {
                transferFile(resolvedRemote, local.toPath(), client);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          } else {
            return processScp(scpClient -> {
              try {
                scpClient.download(resolvedRemote, local.toPath());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return true;
    }
  }

  public boolean scp(File local, String remote, String workDir) throws Exception {
    synchronized (sessionWrapper) {
      try {
        final String resolvedRemote = resolvePath(remote, workDir);
        if (local.isDirectory()) {
          mkdir(resolvedRemote);
          if (MODE_SFTP) {
            processSftp(client -> {
              try {
                for (File file : local.listFiles()) {
                  transferFiles(file, resolvedRemote, client);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          } else {
            return processScp(scpClient -> {
              try {
                scpClient.upload(local.toPath(), resolvedRemote, ScpClient.Option.Recursive, ScpClient.Option.TargetIsDirectory);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          }
        } else {
          mkdir(Paths.get(resolvedRemote).getParent().toString());
          if (MODE_SFTP) {
            processSftp(client -> {
              try {
                transferFile(local, resolvedRemote, client);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          } else {
            return processScp(scpClient -> {
              try {
                scpClient.upload(local.toPath(), resolvedRemote);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            });
          }
        }
      } catch (Exception e) {
        //WarnLogMessage.issue(loggingTarget, e);
        //return false;
        throw new RuntimeException(e);
      }
      return true;
    }
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
    outputStream.flush();
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

  public void close() throws Exception {
    disconnect(true);
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
            if ( e.toString().contains("Channel is being closed")
              || e.toString().contains("channel is not opened")
              || e.toString().contains("SSH_FX_FAILURE") ) {
              isFailed = true;
              provider.failed();
            } else {
              throw e;
            }
          }
          provider.close();
        }
      } while (isFailed);
      return result;
  }

  private boolean processScp(Function<ScpClient, Boolean> process) throws Exception {
    boolean result = false;
    boolean isFailed;
    do {
      isFailed = false;

      try (ChannelProvider provider = new ChannelProvider()) {
        try {
          result = process.apply(provider.createScpChannel());
          provider.close();
        } catch (Exception e) {
          if (Main.hibernatingFlag) {
            throw e;
          }
          if ( e.toString().contains("Channel is being closed")
            || e.toString().contains("channel is not opened")
            || e.toString().contains("SSH_FX_FAILURE") ) {
            e.printStackTrace();
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
           ChannelExec channel = provider.createExecChannel(fullCommand, stdoutOutputStream, stderrOutputStream)) {

        int timeoutCount = 0;
        while (LONG_TIMEOUT > TIMEOUT * timeoutCount) {
          try {
            channel.waitFor(EnumSet.of(ClientChannelEvent.EXIT_STATUS), Duration.ofSeconds(TIMEOUT));
            exitStatus = channel.getExitStatus();
            break;
          } catch (RuntimeException e) {
            InfoLogMessage.issue(loggingTarget, "waiting a response");
            timeoutCount += 1;
            if (LONG_TIMEOUT <= TIMEOUT * timeoutCount) {
              throw new FailedToControlRemoteException(e);
            }
          }
        }

        stdout = stdoutOutputStream.toString();
        stderr = stderrOutputStream.toString();
      }
      return this;
    }
  }

  static int count1 = 0;
  static int count2 = 0;
  static int count3 = 0;
  class ChannelProvider implements AutoCloseable {
    int failedCount;
    boolean isSessionClosed;
    ChannelExec channelExec;
    //SftpClient sftpClient;
    boolean isSftp;
    DebugElapsedTime debugElapsedTime;
    boolean isClosed = false;

    public ChannelProvider() {
      debugElapsedTime = new DebugElapsedTime("NULL: ");
      failedCount = 0;
      isSessionClosed = false;
      channelExec = null;
      //sftpClient = null;
      isSftp = false;
    }

    public ChannelExec createExecChannel(String command, OutputStream stdoutOutputStream, OutputStream stderrOutputStream) {
      debugElapsedTime = new DebugElapsedTime("EXEC " + (++count1) + ": ");
      boolean isFailed = false;
      do {
        isFailed = false;
        checkSession();
        try {
          channelExec = sessionWrapper.get().createExecChannel(command);
          channelExec.setOut(stdoutOutputStream);
          channelExec.setErr(stderrOutputStream);
          channelExec.open().verify(TIMEOUT, TimeUnit.SECONDS);
        } catch (IOException e) {
          isFailed = true;
          failed();
        }
      } while (isFailed);
      return channelExec;
    }

    public ScpClient createScpChannel() {
      debugElapsedTime = new DebugElapsedTime("SCP: ");
      ScpClient scpClient = null;
      boolean isFailed = false;
      do {
        isFailed = false;
        checkSession();
        try {
          scpClient = ScpClientCreator.instance().createScpClient(sessionWrapper.get());
        } catch (NullPointerException e) {
          isFailed = true;
          failed();
        }
      } while (isFailed);
      return scpClient;
    }

    public SftpClient createSftpChannel() {
      debugElapsedTime = new DebugElapsedTime("EXEC " + (++count3) + ": ");
      isSftp = true;
      boolean isFailed = false;
      do {
        isFailed = false;
        try {
          checkSession();
          if (sessionWrapper.sftpClient == null || !sessionWrapper.sftpClient.isOpen()) {
            if (sessionWrapper.sftpClient != null) {
              try {
                sessionWrapper.sftpClient.close();
              } catch (Throwable e) {
                //NOP
              }
            }
            sessionWrapper.sftpClient = SftpClientFactory.instance().createSftpClient(sessionWrapper.get()).singleSessionInstance();
          }
        } catch (IOException e) {
          isFailed = true;
          failed();
        }
      } while (isFailed);
      return sessionWrapper.sftpClient;
    }

    private void checkSession() {
      if (!isConnected()) {
        //isSessionClosed = true;
        try {
          connect(true);
        } catch (IOException e) {
          throw new FailedToAcquireConnectionException(e);
        }
      }
    }

    public void failed() {
      failedCount += 1;
      if (failedCount < 0) {
        resetChannel(true);
        //InfoLogMessage.issue(loggingTarget, "Retry to open channel after 1 sec.");
        Simple.sleep(TimeUnit.SECONDS, 1);
      } else {
        System.out.println("Failed to open channel: " + count1 + " + " + count2 + " + " + count3 + " = " + (count1 + count2 + count3));
        count1 = count2 = count3 = 0;
        resetSession();
        int sleepTime = Math.min(60, failedCount);
        InfoLogMessage.issue(loggingTarget, "Reset the session and retry to open channel after " + sleepTime + " sec.");
        Simple.sleep(TimeUnit.SECONDS, sleepTime);
      }
    }

    private void resetChannel(boolean resetSftp) {
      if (sessionWrapper.sftpClient != null && resetSftp) {
        try {
          sessionWrapper.sftpClient.close();
          sessionWrapper.sftpClient.getChannel().close(false).awaitUninterruptibly();
          while (!sessionWrapper.sftpClient.getChannel().isClosed()) {
            Simple.sleep(TimeUnit.MILLISECONDS, 10);
          }
        } catch (IOException e) {
          //NOP
        }
        sessionWrapper.sftpClient = null;
      }

      if (channelExec != null) {
        channelExec.close(false).awaitUninterruptibly();
        channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), Duration.ofSeconds(TIMEOUT));
        while (!channelExec.isClosed()) {
          Simple.sleep(TimeUnit.MILLISECONDS, 10);
        }
        channelExec = null;
      }

      System.out.println("CH: " + ((AbstractConnectionService)sessionWrapper.get().getService(ConnectionService.class)).getChannels().size());
    }

    private void resetSession() {
      resetChannel(true);
      disconnect(false);
    }

    @Override
    public void close() throws Exception {
      if (!isClosed) {
        resetChannel(false);

        if (isSessionClosed) {
          disconnect(true);
        }

        if (System.getenv("DEBUG") != null) {
          if (System.getenv("DEBUG").equals("1")) {
            debugElapsedTime.print();
          } else if (System.getenv("DEBUG").equals("2")) {
            debugElapsedTime.print();
            try {
              throw new Exception("DEBUG MESSAGE");
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }

        isClosed = true;
      }
    }
  }
}
