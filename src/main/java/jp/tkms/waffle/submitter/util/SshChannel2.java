package jp.tkms.waffle.submitter.util;

import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SshChannel2 {
  Session.Command command;
  String stdout;
  String stderr;

  public SshChannel2(Session.Command command) throws IOException {
    this.command = command;

    BufferedInputStream stdoutStream = new BufferedInputStream(command.getInputStream());
    BufferedInputStream stderrStream = new BufferedInputStream(command.getErrorStream());

    ByteArrayOutputStream stdoutOutputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream stderrOutputStream = new ByteArrayOutputStream();

    byte[] buf = new byte[1024];
    int len = 0;
    while (stdoutStream != null || stderrStream != null) {
      if (stdoutStream != null) {
        len = stdoutStream.read(buf);
        if (len < 0) {
          stdoutStream.close();
          stdoutStream = null;
          break;
        }
        stdoutOutputStream.write(buf, 0, len);
      }

      if (stderrStream != null) {
        len = stderrStream.read(buf);
        if (len < 0) {
          stderrStream.close();
          stderrStream = null;
          break;
        }
        stderrOutputStream.write(buf, 0, len);
      }
    }

    stdout = new String(stdoutOutputStream.toByteArray(), StandardCharsets.UTF_8);
    stderr = new String(stderrOutputStream.toByteArray(), StandardCharsets.UTF_8);
    stdoutOutputStream.close();
    stderrOutputStream.close();
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public Session.Command toCommand() {
    return command;
  }
}
