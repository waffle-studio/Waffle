package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.value.ObjectWrapper;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;

import java.util.HashSet;

public class SessionWrapperMina extends SessionWrapper<SshSessionMina, ClientSession> {
  private HashSet<SshSessionMina> sessionSet = new HashSet<>();
  SftpClient sftpClient;

  public void link(SshSessionMina sshSession) {
    synchronized (sessionSet) {
      sessionSet.add(sshSession);
    }
  }

  public boolean unlink(SshSessionMina sshSession) {
    synchronized (sessionSet) {
      sessionSet.remove(sshSession);
      if (sessionSet.isEmpty()) {
        set(null);
      }
      return sessionSet.isEmpty();
    }
  }

  public int size() {
    return sessionSet.size();
  }
}
