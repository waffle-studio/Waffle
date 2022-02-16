package jp.tkms.waffle.communicator.util;

import com.jcraft.jsch.Session;
import jp.tkms.util.ObjectWrapper;

import java.util.HashSet;

public class SessionWrapper extends ObjectWrapper<Session> {
  private HashSet<SshSession> sessionSet = new HashSet<>();

  public void link(SshSession sshSession) {
    synchronized (sessionSet) {
      sessionSet.add(sshSession);
    }
  }

  public boolean unlink(SshSession sshSession) {
    synchronized (sessionSet) {
      sessionSet.remove(sshSession);
      if (sessionSet.isEmpty()) {
        setValue(null);
      }
      return sessionSet.isEmpty();
    }
  }

  public int size() {
    return sessionSet.size();
  }
}
