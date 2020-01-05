package jp.tkms.aist;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class AbciSshSession extends SshSession {
    final String keyStore = "/home/takami/.ssh/abci";

    private Session sessionAs;

    public AbciSshSession() throws JSchException {
        super();

        jsch.addIdentity(keyStore + "/id_rsa");
        sessionAs = jsch.getSession("aaa10259dp", "as.abci.ai", 22);
        sessionAs.setConfig("StrictHostKeyChecking", "no");
        sessionAs.connect();
        int assingedPort = sessionAs.setPortForwardingL(0, "es", 22);

        session = jsch.getSession("aaa10259dp", "localhost", assingedPort);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        sessionAs.disconnect();
    }
}
