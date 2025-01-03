package jp.tkms.waffle.data.computer;

import jp.tkms.utils.abbreviation.Simple;
import jp.tkms.utils.string.HashString;
import jp.tkms.utils.crypt.AES;
import jp.tkms.utils.crypt.DecryptingException;
import jp.tkms.utils.crypt.EncryptingException;
import jp.tkms.waffle.Main;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.web.Password;
import jp.tkms.waffle.data.web.UserSession;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class MasterPassword {
  private static String masterPassword = null;

  public static boolean isRegistered() {
    return masterPassword != null;
  }

  public static void register(String password) {
    masterPassword = HashString.toSHA256Base64(UserSession.getWaffleId() + password);
  }

  public static boolean registerWithAuthenticate(String password) {
    if (password != null && Password.authenticate(password)) {
      register(password);
      return true;
    }
    return false;
  }

  public static String getEncrypted(String value) throws InterruptedException {
    waitForPreparing();
    try {
      return AES.encryptToString(masterPassword, value);
    } catch (EncryptingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getDecrypted(String value) throws InterruptedException {
    waitForPreparing();
    try {
      return AES.decryptToString(masterPassword, value);
    } catch (DecryptingException e) {
      throw new RuntimeException(e);
    }
  }

  private static void waitForPreparing() throws InterruptedException {
    try {
      if (masterPassword == null) { InfoLogMessage.issue("WAIT TO INPUT YOUR MASTER PASSWORD. You should login from a web browser, the command 'pass <password>', or the environment value 'MASTER_PASS'."); }
      Simple.waitUntil(()-> masterPassword == null && !Main.hibernatingFlag, TimeUnit.SECONDS, 1);
      if (masterPassword == null) { throw new InterruptedException(); }
    } catch (InterruptedException e) {
      InfoLogMessage.issue("Abort a decrypting/encrypting process by a master password.");
      throw e;
    }
  }
}
