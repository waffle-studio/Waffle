package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.value.ObjectWrapper;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;

import java.util.HashSet;

public abstract class SessionWrapper<T,S> extends ObjectWrapper<S> {
  private HashSet<T> sessionSet = new HashSet<>();

  public void link(T sshSession) {
    synchronized (sessionSet) {
      sessionSet.add(sshSession);
    }
  }

  public boolean unlink(T sshSession) {
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
