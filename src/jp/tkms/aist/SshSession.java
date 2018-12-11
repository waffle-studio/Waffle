package jp.tkms.aist;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

    public boolean mkdir(String path, String workDir) throws JSchException {
        SshChannel channel = exec("mkdir -p " + path, workDir);

        return (channel.getExitStatus() == 0);
    }

    public boolean scp(File local, String dest, String workDir) throws JSchException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        try {
            channelSftp.cd(workDir);
            try {
                channelSftp.mkdir(dest);
            } catch (SftpException e) {}
            channelSftp.cd(dest);
            for(File file: local.listFiles()){
                transferFiles(file, dest, channelSftp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        channelSftp.disconnect();
        return true;
    }

    private static void transferFiles(File localFile, String destPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
        if(localFile.isDirectory()){
            try {
                clientChannel.mkdir(localFile.getName());
            } catch (SftpException e) {}

            destPath = destPath + "/" + localFile.getName();
            clientChannel.cd(destPath);

            for(File file: localFile.listFiles()){
                transferFiles(file, destPath, clientChannel);
            }
            clientChannel.cd("..");
        } else {
            transferFile(localFile, localFile.getName(), clientChannel);
        }
    }

    private static void transferFile(File localFile, String destPath, ChannelSftp clientChannel) throws SftpException, FileNotFoundException {
        clientChannel.put(new FileInputStream(localFile), destPath,ChannelSftp.OVERWRITE);
    }
}
