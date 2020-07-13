package jp.tkms.waffle.submitter.util;

import jp.tkms.waffle.data.log.WarnLogMessage;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.DirectConnection;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.xfer.FileSystemFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class SshSession2 {
  private final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.ssh/config";
  private final String DEFAULT_PRIVKEY_FILE = System.getProperty("user.home") + "/.ssh/id_rsa";
  protected SSHClient sshClient;
  protected Session session;
  protected String username;
  protected String host;
  protected int port;
  protected FileKeyProvider fileKeyProvider;

  public SshSession2() {
    sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new PromiscuousVerifier());
  }

  public boolean isConnected() {
    if (session != null) {
      return session.isOpen();
    }
    return false;
  }

  public void addIdentity(String privKey) {
    fileKeyProvider = new OpenSSHKeyFile();
    fileKeyProvider.init(Paths.get(privKey).toFile());
  }

  public void addIdentity(String privKey, String pass) {
    fileKeyProvider = new OpenSSHKeyFile();
    fileKeyProvider.init(Paths.get(privKey).toFile(), PasswordUtils.createOneOff(pass.toCharArray()));
  }

  public void setSession(String username , String host, int port) {
    this.username = username;
    this.host = host;
    this.port = port;
  }

  public SSHClient getSshClientAfterConnect(SSHClient tunnel) throws IOException {
    if (tunnel == null) {
      sshClient.connect(host, port);
      if (fileKeyProvider == null) {
        sshClient.authPublickey(username);
      } else {
        sshClient.authPublickey(username, fileKeyProvider);
      }
    } else {
      sshClient.connectVia(tunnel.newDirectConnection(host, port));
    }
    return sshClient;
  }

  public SSHClient getSshClientAfterConnect() throws IOException {
    return getSshClientAfterConnect(null);
  }

  public void connect(boolean retry, SSHClient tunnel) throws IOException {
    boolean connected = false;
    int waitTime = 10;
    do {
      try {
        if (tunnel == null) {
          sshClient.connect(host, port);
        } else {
          sshClient.connectVia(tunnel.newDirectConnection(host, port));
        }
        session = sshClient.startSession();
        connected = true;
      } catch (IOException e) {
        if (!retry) {
          WarnLogMessage.issue(e.getMessage());
          throw e;
        } else if (!e.getMessage().toLowerCase().equals("session is already connected")) {
          WarnLogMessage.issue(e.getMessage() + "\nRetry connection after " + waitTime + " sec.");
          disconnect();
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

  public void connect(boolean retry) throws IOException {
    connect(retry, null);
  }

  public void disconnect() {
    try {
      session.close();
    } catch (TransportException | ConnectionException e) {
      e.printStackTrace();
    }
    try {
      sshClient.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SshChannel2 exec(String command, String workDir) throws IOException {
    String submittingCommand = "cd " + workDir + " && " + command;
    Session.Command channel = session.exec("sh -c '" +  submittingCommand.replaceAll("'", "'\\\\''") + "'\n");
    channel.join();
    return new SshChannel2(channel);
  }

  public boolean mkdir(String path, String workDir) throws IOException {
    Session.Command channel = exec("mkdir -p " + path, workDir).toCommand();

    return (channel.getExitStatus() == 0);
  }

  public boolean rmdir(String path, String workDir) throws IOException {
    Session.Command channel = exec("rm -rf " + path, workDir).toCommand();

    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws IOException {
    Session.Command channel = exec("cat " + path, workDir).toCommand();

    BufferedInputStream outStream = new BufferedInputStream(channel.getInputStream());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    while (true) {
      int len = outStream.read(buf);
      if (len <= 0) {
        break;
      }
      outputStream.write(buf, 0, len);
    }

    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
  }

  public void putText(String text, String path, String workDir) throws IOException {
    SFTPClient sftpClient = sshClient.newSFTPClient();
    RemoteFile file = sftpClient.open(workDir.concat("/").concat(path));
    file.write(0, text.getBytes(), 0, text.getBytes().length);
    file.close();
    sftpClient.close();
    return;
  }

  public synchronized void scp(String remote, File local, String workDir) throws IOException {
    SFTPClient sftpClient = sshClient.newSFTPClient();
    transferFiles(workDir.concat("/").concat(remote), local.toPath(), sftpClient);
    sftpClient.close();
  }

  public synchronized void scp(File local, String remote, String workDir) throws IOException {
    SFTPClient sftpClient = sshClient.newSFTPClient();
    transferFiles(local, workDir.concat("/").concat(remote), sftpClient);
    sftpClient.close();
  }

  private static void transferFiles(String remotePath, Path localPath, SFTPClient clientChannel) throws IOException {
    String name = Paths.get(remotePath).getFileName().toString();
    if(clientChannel.stat(remotePath).getType() == FileMode.Type.DIRECTORY){
      Files.createDirectories(localPath.resolve(name));

      for(RemoteResourceInfo entry: clientChannel.ls(remotePath)){
        transferFiles(entry.getName(), localPath.resolve(name), clientChannel);
      }
    } else {
      transferFile(remotePath, localPath.resolve(name), clientChannel);
    }
  }

  private static void transferFile(String remotePath, Path localPath, SFTPClient clientChannel) throws IOException {
    clientChannel.get(remotePath, new FileSystemFile(localPath.toString()));
  }

  private static void transferFiles(File localFile, String destPath, SFTPClient clientChannel) throws IOException, FileNotFoundException {
    System.out.println(localFile + "   --->>>  " + destPath);
    if(localFile.isDirectory()){
      clientChannel.mkdir(localFile.getName());

      destPath = destPath + "/" + localFile.getName();

      for(File file: localFile.listFiles()){
        transferFiles(file, destPath, clientChannel);
      }
    } else {
      transferFile(localFile, localFile.getName(), clientChannel);
    }
  }

  private static void transferFile(File localFile, String destPath, SFTPClient clientChannel) throws IOException, FileNotFoundException {
    clientChannel.put(localFile.getAbsolutePath(), destPath);

    String perm = localExec("stat '" + localFile.getAbsolutePath() + "' -c '%a'").replaceAll("\\r|\\n", "");
    clientChannel.chmod(destPath, Integer.parseInt(perm, 8));
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
