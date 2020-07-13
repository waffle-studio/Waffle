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

    BufferedInputStream outStream = new BufferedInputStream(command.getInputStream());
    BufferedInputStream errStream = new BufferedInputStream(command.getErrorStream());

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    while (true) {
      int len = outStream.read(buf);
      if (len <= 0) {
        break;
      }
      outputStream.write(buf, 0, len);
    }
    stdout = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

    outputStream = new ByteArrayOutputStream();
    buf = new byte[1024];
    while (true) {
      int len = errStream.read(buf);
      if (len <= 0) {
        break;
      }
      outputStream.write(buf, 0, len);
    }
    stderr = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
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
