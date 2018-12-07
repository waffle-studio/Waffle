package jp.tkms.aist;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshSession {
    protected JSch jsch;
    protected Session session;

    public SshSession() throws JSchException {
        this.jsch = new JSch();
    }

    public void disconnect() {
        session.disconnect();
    }

    public SshChannel exec(String command, String workDir) throws JSchException {
        SshChannel channel = new SshChannel((ChannelExec) session.openChannel("exec"));
        channel.exec(command, workDir);
        return channel;
    }
}
