package jp.tkms.waffle.communicator.util;

import jp.tkms.utils.value.ObjectWrapper;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;

import java.util.HashSet;

public class SessionWrapperMina extends SessionWrapper<SshSessionMina, ClientSession> {
  SftpClient sftpClient;
}
