package jp.tkms.waffle.data.web;

import jp.tkms.utils.crypt.AES;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.MasterPassword;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Password {
  public static void setPassword(String password) {
    if (isNotEmpty(password)) {
      try {
        Path passwordFilePath = getPasswordFilePath();
        Files.writeString(passwordFilePath, "$5$" + Base64.getEncoder().encodeToString(AES.toSHA256(password)));
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }
  }

  public static boolean isNotEmpty() {
    return isNotEmpty(getPasswordHash());
  }

  public static boolean isNotEmpty(String password) {
    return !"".equals(password);
  }

  public static String getPasswordHash() {
    Path passwordFilePath = getPasswordFilePath();
    if (!passwordFilePath.toFile().exists()) {
      try {
        Files.writeString(passwordFilePath, "");
      } catch (IOException e) {
        ErrorLogMessage.issue(e);
      }
    }

    String password = null;
    try {
      password = Files.readString(getPasswordFilePath()).trim();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }

    return password;
  }

  public static boolean authenticate(String password) {
    if (getPasswordHash().equals("$5$" + Base64.getEncoder().encodeToString(AES.toSHA256(password)))) {
      MasterPassword.register(password);
      return true;
    }
    return false;
  }

  private static Path getPasswordFilePath() {
    return Constants.WORK_DIR.resolve(Constants.PASSWORD);
  }
}
