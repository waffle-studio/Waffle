package jp.tkms.waffle.data.util;

import jp.tkms.waffle.data.log.message.WarnLogMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndirectValue {
  /**
   * Syntax:
   * [*WAFFLE-<TYPE>::<GLOBAL KEY>::<LOCAL KEY>]
   *   TYPE:
   *      Variable: V
   *      Parameter: P
   *      Result: R
   *   GLOBAL KEY:
   *     Local path
   *   LOCAL KEY:
   *     Json path
   */

  public static final String PREFIX = "[*WAFFLE-";
  public static final String SUFFIX = "]";
  public static final String DELIMITER = "::";
  public static final Pattern PATTERN = Pattern.compile((PREFIX + "(.)" + DELIMITER + "(.+)" + "(.+)" + SUFFIX).replaceAll("([\\[\\]\\*\\-])","\\\\$1"));

  private String type;
  private String globalKey;
  private String localKey;

  public IndirectValue(String type, String globalKey, String localKey) {
    this.type = type;
    this.globalKey = globalKey;
    this.localKey = localKey;
  }

  public String getKey() {
    return PREFIX + type + DELIMITER + globalKey + DELIMITER + localKey + SUFFIX;
  }

  public String getString(String defaults) {
    return null;
  }

  public String getString() {
    return getString(null);
  }

  public static IndirectValue resolve(String key) throws WarnLogMessage {
    Matcher matcher = PATTERN.matcher(key);
    if (matcher.find() && matcher.groupCount() == 3) {
      IndirectValue indirectValue = new IndirectValue(matcher.group(1).toUpperCase(), matcher.group(2), matcher.group(3));
      return indirectValue;
    }
    throw new WarnLogMessage("Invalid indirect value key: " + key);
  }
}
