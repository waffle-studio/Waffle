package jp.tkms.aist;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SshSession {
    private final int waitTime = 100;
    private final int maxOfWait = 100;

    protected JSch jsch;
    protected Session session;
    protected String workDir;

    private String submittedCommand;
    private String stdout;
    private String stderr;
    private int exitStatus;

    public SshSession() throws JSchException {
        this.jsch = new JSch();
        workDir = "~/";
        stdout = "";
        stderr = "";
        exitStatus = -1;
    }

    public void disconnect() {
        session.disconnect();
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
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

    public int exec(String command) throws JSchException {
        stdout = "";
        stderr = "";
        exitStatus = -1;

        ChannelExec channel = (ChannelExec)session.openChannel("exec");

        submittedCommand = "cd " + workDir + " && " + command;
        channel.setCommand(submittedCommand);
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

        return exitStatus = channel.getExitStatus();
    }
}
