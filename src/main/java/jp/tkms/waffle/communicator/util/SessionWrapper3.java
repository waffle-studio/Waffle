package jp.tkms.waffle.communicator.util;

import com.jcraft.jsch.Session;
import jp.tkms.utils.value.ObjectWrapper;

import java.util.HashSet;

public class SessionWrapper3 extends ObjectWrapper<Session> {
  private HashSet<SshSession3> sessionSet = new HashSet<>();

  public void link(SshSession3 sshSession) {
    synchronized (sessionSet) {
      sessionSet.add(sshSession);
    }
  }

  public boolean unlink(SshSession3 sshSession) {
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
