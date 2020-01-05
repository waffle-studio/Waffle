package jp.tkms.waffle.submitter.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SshChannel {
  private final int waitTime = 25;
  private final int maxOfWait = 400;

  private ChannelExec channel;

  private String submittedCommand;
  private String stdout;
  private String stderr;
  private int exitStatus;

  public SshChannel(ChannelExec channel) throws JSchException {
    this.channel = channel;
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

  public SshChannel exec(String command, String workDir) throws JSchException {
    submittedCommand = "cd " + workDir + " && " + command;
    channel.setCommand("sh -c '" +  submittedCommand.replaceAll("'", "'\\\\''") + "'");
    channel.connect();

    try {
      BufferedInputStream outStream = new BufferedInputStream(channel.getInputStream());
      BufferedInputStream errStream = new BufferedInputStream(channel.getErrStream());

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
    } catch (IOException e) {
      e.printStackTrace();
    }

    channel.disconnect();
    int sleepCount = 0;
    do {
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) { }
    } while (!channel.isClosed() && sleepCount++ < maxOfWait);

    exitStatus = channel.getExitStatus();

    return this;
  }
}
