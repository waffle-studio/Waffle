package jp.tkms.waffle.data.web;

import jp.tkms.utils.file.NewFile;
import jp.tkms.utils.string.HashString;
import jp.tkms.waffle.Constants;
import jp.tkms.waffle.data.computer.MasterPassword;
import jp.tkms.waffle.data.log.message.ErrorLogMessage;
import jp.tkms.waffle.data.util.StringFileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Password {
  final static String SHA_PREFIX = "$5$";

  public static void setPassword(String password) {
    if (isNotEmpty(password)) {
      StringFileUtil.write(getPasswordFilePath(), SHA_PREFIX + HashString.toSHA256Base64(password));
    }
  }

  public static boolean isNotEmpty() {
    return isNotEmpty(getPasswordHash());
  }

  public static boolean isNotEmpty(String password) {
    return !"".equals(password);
  }

  public static String getPasswordHash() {
    String password = null;
    try {
      NewFile.createIfNotExists(getPasswordFilePath());
      password = StringFileUtil.read(getPasswordFilePath()).trim();
    } catch (IOException e) {
      ErrorLogMessage.issue(e);
    }
    return password;
  }

  public static boolean authenticate(String password) {
    if (
      getPasswordHash().equals(SHA_PREFIX + HashString.toSHA256Base64(password)) || //SHA256
      getPasswordHash().equals(password) //OLD: Plain
    ) {
      MasterPassword.register(password);
      return true;
    }
    return false;
  }

  private static Path getPasswordFilePath() {
    return Constants.WORK_DIR.resolve(Constants.PASSWORD);
  }
}
