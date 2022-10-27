package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.io.IOStreamUtil;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.computer.Computer;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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


public class SshSession {
  static final int TIMEOUT = 15;
  private static final Map<String, SessionWrapper> sessionCache = new HashMap<>();

  static SshClient client = null;
  private SessionWrapper sessionWrapper = null;
  Semaphore channelSemaphore = new Semaphore(4);
  String username;
  String host;
  int port;
  Computer loggingTarget;
  SshSession3 tunnelSession;

  private String tunnelTargetHost;
  private int tunnelTargetPort;

  public SshSession(Computer loggingTarget, SshSession3 tunnelSession) {
    this.loggingTarget = loggingTarget;
    this.tunnelSession = tunnelSession;

    initialize();
  }

  static void initialize() {
    if (client == null) {
      client = SshClient.setUpDefaultClient();
      client.start();
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
    for (KeyPair keyPair : SecurityUtils.getKeyPairResourceParser().loadKeyPairs(null, Paths.get(privKeyPath), passwordProvider)) {
      client.addPublicKeyIdentity(keyPair);
    }
  }

  public void setSession(String username , String host, int port) {
    this.username = username;
    this.host = host;
    this.port = port;
  }

  public void start() {
    SshClient client = SshClient.setUpDefaultClient();
// override any default configuration...
    //client.setSomeConfiguration(...);
    //client.setOtherConfiguration(...);
    client.start();

    String user = "takami";
    String host = "localhost";
    int port = 22;

    // using the client for multiple sessions...
    System.out.println("@GET SESSION");
    try (ClientSession session = client.connect(user, host, port).verify(5, TimeUnit.SECONDS).getSession()) {

      for (KeyPair keyPair : SecurityUtils.getKeyPairResourceParser().loadKeyPairs(null, Paths.get("/home/takami/.ssh/iso/id_rsa"), FilePasswordProvider.of("stakami"))) {
        session.addPublicKeyIdentity(keyPair);
      }

      //session.addPublicKeyIdentity();
      //session.addPasswordIdentity(...password..); // for password-based authentication
      //session.addPublicKeyIdentity(...key-pair...); // for password-less authentication
      // Note: can add BOTH password AND public key identities - depends on the client/server security setup

      System.out.println("@VERIFY");
      session.auth().verify(5, TimeUnit.SECONDS);
      // start using the session to run commands, do SCP/SFTP, create local/remote port forwarding, etc...

      String command = "ls /";

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (OutputStream stdout = byteArrayOutputStream;
           OutputStream stderr = byteArrayOutputStream;
           ClientChannel channel = session.createExecChannel(command)) {
        channel.setOut(stdout);
        channel.setErr(stderr);
        channel.open().verify(5, TimeUnit.SECONDS);
        // Wait (forever) for the channel to close - signalling command finished
        channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0L);

        System.out.println(byteArrayOutputStream.toString());
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    System.out.println("@END");

// exiting in an orderly fashion once the code no longer needs to establish SSH session
// NOTE: this can/should be done when the application exits.
    client.stop();
  }

  public void connect(boolean retry) throws IOException {
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

          if (e.getMessage().toLowerCase().equals("userauth fail")) {
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
    }
  }

  ClientChannel openChannel(String type) throws IOException, InterruptedException {
    return sessionWrapper.get().createChannel(type);
  }

  public int setPortForwardingL(String hostName, int rport) throws IOException {
    tunnelTargetHost = hostName;
    tunnelTargetPort = rport;
    return sessionWrapper.get().startLocalPortForwarding(0, new SshdSocketAddress(hostName, rport)).getPort();
  }

  public SshChannel exec(String command, String workDir) throws InterruptedException, IOException {
    synchronized (sessionWrapper) {
      SshChannel channel = new SshChannel();
      int count = 0;
      boolean failed = false;
      do {
        try {
          channel.exec(command, workDir);
        } catch (IOException e) {
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

  public boolean chmod(int mod, Path path) throws IOException {
    return processSftp(sftpClient -> {
      try {
        SftpClient.Attributes attributes = sftpClient.stat(path.toString());
        attributes.setPermissions(Integer.parseInt("" + mod, 8));
        sftpClient.setStat(path.toString(), attributes);
      } catch (IOException e) {
        return false;
      }
      return true;
    });
  }

  public boolean exists(Path path) throws IOException {
    if (path == null) {
      return false;
    }
    return processSftp(sftpClient -> {
      try {
        sftpClient.stat(path.toString());
      } catch (IOException e) {
        return false;
      }
      return true;
    });
  }

  public boolean mkdir(Path path) throws IOException {
    return processSftp(sftpClient -> {
      try {
        sftpClient.stat(path.getParent().toString());
      } catch (IOException e) {
        if (e.getMessage().startsWith("No such file")) {
          try {
            mkdir(path.getParent());
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
        sftpClient.stat(path.toString());
        return true;
      } catch (IOException e) {
        if (e.getMessage().startsWith("No such file")) {
          try {
            sftpClient.mkdir(path.toString());
          } catch (IOException ex) {
            WarnLogMessage.issue(loggingTarget, ex);
            return false;
          }
        }
        else {
          e.printStackTrace();
        }
      }
      return true;
    });
  }

  public boolean rmdir(String path, String workDir) throws IOException, InterruptedException {
    SshChannel channel = exec("rm -rf " + path, workDir);

    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws IOException {
    //SshChannel channel = exec("cat " + path, workDir);

    //return channel.getStdout();
    final String[] resultText = new String[1];
    processSftp(sftpClient -> {
      try {
        String finalPath = path;
        if (workDir != null && !"".equals(workDir)) {
          finalPath = Paths.get(workDir).resolve(path).normalize().toString();
        }
        InputStream inputStream = sftpClient.read(finalPath);
        resultText[0] = IOStreamUtil.readString(inputStream);
        inputStream.close();
      } catch (IOException e) {
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
        String finalPath = path;
        if (workDir != null && !"".equals(workDir)) {
          finalPath = Paths.get(workDir).resolve(path).normalize().toString();
        }
        OutputStream outputStream = this.sftpClient.write(finalPath);
        outputStream.write(text.getBytes());
        outputStream.close();
      } catch (IOException e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean rm(Path path) throws IOException {
    return processSftp(sftpClient -> {
      try {
        sftpClient.remove(path.toString());
      } catch (IOException e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  public synchronized boolean scp(String remote, File local, String workDir) throws IOException {
    return processSftp(sftpClient -> {
      try {
        String finalRemote = remote;
        if (workDir != null && !"".equals(workDir)) {
          finalRemote = Paths.get(workDir).resolve(remote).normalize().toString();
        }
        if(sftpClient.stat(finalRemote).isDirectory()) {
          Files.createDirectories(local.toPath());
          for (SftpClient.DirEntry entry : sftpClient.readDir(finalRemote)) {
            transferFiles(entry.getFilename(), local.toPath(), sftpClient);
          }
        } else {
          Files.createDirectories(local.toPath().getParent());
          transferFile(finalRemote, local.toPath(), sftpClient);
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
        String finalRemote = remote;
        if (workDir != null && !"".equals(workDir)) {
          finalRemote = Paths.get(workDir).resolve(remote).normalize().toString();
        }
        if (local.isDirectory()) {
          mkdir(Paths.get(finalRemote));
          for(File file: local.listFiles()){
            transferFiles(file, finalRemote, channelSftp);
          }
        } else {
          transferFile(local, finalRemote, channelSftp);
        }
      } catch (Exception e) {
        WarnLogMessage.issue(loggingTarget, e);
        return false;
      }
      return true;
    });
  }

  private static void transferFiles(String remotePath, Path localPath, SftpClient sftpClient) throws IOException {
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void transferFiles(File localFile, String destPath, SftpClient sftpClient) throws IOException {
    System.out.println(localFile + "   --->>>  " + destPath);
    if(localFile.isDirectory()){
      try {
        sftpClient.mkdir(localFile.getName());
      } catch (SftpException e) {}

      destPath = destPath + "/" + localFile.getName();

      for(File file: localFile.listFiles()){
        transferFiles(file, destPath, sftpClient);
      }
    } else {
      transferFile(localFile, localFile.getName(), sftpClient);
    }
  }

  private static void transferFile(File localFile, String destPath, SftpClient sftpClient) throws IOException {
    OutputStream outputStream = sftpClient.write(destPath, SftpClient.OpenMode.Write);
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

  public class SshChannel {
    private String submittedCommand;
    private String stdout;
    private String stderr;
    private int exitStatus;

    public SshChannel() {
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

    public SshChannel exec(String command, String workDir) throws InterruptedException, IOException {
      submittedCommand = "cd " + workDir + " && " + command;
      String fullCommand = "sh -c '" + submittedCommand.replaceAll("'", "'\\\\''") + "'";

      channelSemaphore.acquire();
      try (ByteArrayOutputStream stdoutOutputStream = new ByteArrayOutputStream();
           ByteArrayOutputStream stderrOutputStream = new ByteArrayOutputStream();
           ClientChannel channel = sessionWrapper.get().createExecChannel(fullCommand)) {
        channel.setOut(stdoutOutputStream);
        channel.setErr(stderrOutputStream);
        channel.open().verify(TIMEOUT, TimeUnit.SECONDS);
        // Wait (forever) for the channel to close - signalling command finished
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
