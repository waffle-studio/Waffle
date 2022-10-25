package jp.tkms.waffle.data.computer;

import jp.tkms.utils.crypt.AES;
import jp.tkms.utils.crypt.DecryptingException;
import jp.tkms.utils.crypt.EncryptingException;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.log.message.InfoLogMessage;
import jp.tkms.waffle.data.web.UserSession;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class MasterPassword {
  private static String masterPassword = null;

  public static void register(String password) {
    masterPassword = Base64.getEncoder().encodeToString(AES.toSHA256(UserSession.getWaffleId() + password));
  }

  private static void waitForPreparing() throws InterruptedException {
    try {
      if (masterPassword == null) {
        InfoLogMessage.issue("Wait to input your master password");
      }
      while (masterPassword == null) {
        TimeUnit.SECONDS.sleep(1);
      }
    } catch (InterruptedException e) {
      InfoLogMessage.issue("Abort a crypting process by a master password");
      throw e;
    }
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
}
