package jp.tkms.waffle.submitter.util;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import jp.tkms.waffle.data.log.WarnLogMessage;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.DirectConnection;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.*;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyFormat;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.bouncycastle.jcajce.provider.util.SecretKeyUtil;
import org.bouncycastle.pqc.jcajce.provider.util.KeyUtil;

import java.io.*;
import java.nio.channels.ScatteringByteChannel;
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
  SFTPClient sftpClient;

  protected String username;
  protected String host;
  protected int port;
  protected FileKeyProvider fileKeyProvider;

  public SshSession2() {
    sshClient = new SSHClient();
    sshClient.addHostKeyVerifier(new PromiscuousVerifier());
  }

  public boolean isConnected() {
    if (sshClient != null) {
      return sshClient.isConnected();
    }
    return false;
  }

  public void addIdentity(String privKey) {
    addIdentity(privKey, null);
  }

  public void addIdentity(String privKey, String pass) {
    fileKeyProvider = new OpenSSHKeyFile();
    File privKeyFile = Paths.get(privKey.replaceFirst("^~", System.getProperty("user.home"))).toFile();
    if (pass == null) {
      fileKeyProvider.init(privKeyFile);
    } else {
      fileKeyProvider.init(privKeyFile, PasswordUtils.createOneOff(pass.toCharArray()));
    }
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
        if (fileKeyProvider == null) {
          sshClient.authPublickey(username);
        } else {
          sshClient.authPublickey(username, fileKeyProvider);
        }
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
    if (sftpClient != null) {
      try {
        sftpClient.close();
      } catch (IOException e) {
      }
    }
    if (sshClient != null) {
      try {
        sshClient.close();
      } catch (IOException e) {
      }
    }
  }

  public SshChannel2 exec(String command, String workDir) throws IOException {
    session = sshClient.startSession();
    String submittingCommand = "cd " + workDir + " && " + command;
    Session.Command channel = session.exec("sh -c '" +  submittingCommand.replaceAll("'", "'\\\\''") + "'\n");
    channel.join();
    session.close();
    return new SshChannel2(channel);
  }

  public boolean mkdir(Path path) throws IOException {
    if (sftpClient == null) {
      sftpClient = sshClient.newSFTPClient();
    }
    try {
      sftpClient.stat(path.getParent().toString());
    } catch (SFTPException e) {
      if (e.getMessage().startsWith("No such file")) {
        try {
          mkdir(path.getParent());
        } catch (IOException ex) {
          WarnLogMessage.issue(e);
          return false;
        }
      }
    }
    try {
      sftpClient.stat(path.toString());
      return true;
    } catch (SFTPException e) {
      if (e.getMessage().startsWith("No such file")) {
        try {
          sftpClient.mkdir(path.toString());
        } catch (IOException ex) {
          WarnLogMessage.issue(ex);
          return false;
        }
      }
    }
    return true;
  }

  public boolean rmdir(String path, String workDir) throws IOException {
    Session.Command channel = exec("rm -rf " + path, workDir).toCommand();
    return (channel.getExitStatus() == 0);
  }

  public String getText(String path, String workDir) throws IOException {
    return exec("cat " + path, workDir).getStdout();
  }

  public void putText(String text, String path, String workDir) throws IOException {
    if (sftpClient == null) {
      sftpClient = sshClient.newSFTPClient();
    }
    InMemorySourceFile sourceFile = new InMemorySourceFile() {
      @Override
      public String getName() {
        return "file";
      }

      @Override
      public long getLength() {
        return text.getBytes().length;
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(text.getBytes());
      }
    };
    sftpClient.put(sourceFile, workDir.concat("/").concat(path));
    return;
  }

  public synchronized void scp(String remote, File local) throws IOException {
    if (sftpClient == null) {
      sftpClient = sshClient.newSFTPClient();
    }
    transferFiles(remote, local.toPath(), sftpClient);
  }

  public synchronized void scp(File local, String remote) throws IOException {
    if (sftpClient == null) {
      sftpClient = sshClient.newSFTPClient();
    }
    transferFiles(local, remote, sftpClient);
  }

  private static void transferFiles(String remotePath, Path localPath, SFTPClient clientChannel) throws IOException {
    try {
      String name = Paths.get(remotePath).getFileName().toString();
      if (clientChannel.stat(remotePath).getType() == FileMode.Type.DIRECTORY) {
        Files.createDirectories(localPath.resolve(name));

        for (RemoteResourceInfo entry : clientChannel.ls(remotePath)) {
          transferFiles(entry.getName(), localPath.resolve(name), clientChannel);
        }
      } else {
        transferFile(remotePath, localPath, clientChannel);
      }
    }catch (Exception e) {e.printStackTrace();}
  }

  private static void transferFile(String remotePath, Path localPath, SFTPClient clientChannel) throws IOException {
    clientChannel.get(remotePath, new FileSystemFile(localPath.toString()));
  }

  private void transferFiles(File localFile, String destPath, SFTPClient clientChannel) throws IOException, FileNotFoundException {
    //System.out.println(localFile + "   --->>>  " + destPath);
    if (localFile.isDirectory()) {
      mkdir(Paths.get(destPath));

      for (File file : localFile.listFiles()) {
        destPath = destPath + "/" + file.getName();
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
