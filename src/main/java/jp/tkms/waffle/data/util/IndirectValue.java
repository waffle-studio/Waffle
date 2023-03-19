package jp.tkms.waffle.data.util;

import jnr.ffi.annotations.In;
import jp.tkms.waffle.data.log.message.WarnLogMessage;
import jp.tkms.waffle.data.project.Project;
import jp.tkms.waffle.data.project.workspace.Workspace;
import jp.tkms.waffle.data.project.workspace.run.AbstractRun;
import jp.tkms.waffle.data.project.workspace.run.ConductorRun;
import jp.tkms.waffle.data.project.workspace.run.ExecutableRun;
import jp.tkms.waffle.exception.RunNotFoundException;

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

  public Workspace getWorkspace() {
    return Workspace.resolveFromLocalPathString(globalKey);
  }

  public String getString(String defaults) {
    try {
      switch (type) {
        case "V":
          return ConductorRun.getInstance(getWorkspace(), globalKey).getVariables().getString(localKey, defaults);
        case "P":
          return ExecutableRun.getInstance(globalKey).getParameters().getString(localKey, defaults);
        case "R":
          return ExecutableRun.getInstance(globalKey).getResults().getString(localKey, defaults);
      }
    } catch (RunNotFoundException | NullPointerException e) {
      return defaults;
    }
    return defaults;
  }

  public String getString() {
    return getString(null);
  }

  public static IndirectValue convert(String key) throws WarnLogMessage {
    Matcher matcher = PATTERN.matcher(key);
    if (matcher.find() && matcher.groupCount() == 3) {
      IndirectValue indirectValue = new IndirectValue(matcher.group(1).toUpperCase(), matcher.group(2), matcher.group(3));
      return indirectValue;
    }
    throw new WarnLogMessage("Invalid indirect value key: " + key);
  }

  public static IndirectValue getVariableKey(String localPath, String key) {
    return new IndirectValue("V", localPath, key);
  }

  public static IndirectValue getParameterKey(String localPath, String key) {
    return new IndirectValue("P", localPath, key);
  }

  public static IndirectValue getResultKey(String localPath, String key) {
    return new IndirectValue("R", localPath, key);
  }
}
