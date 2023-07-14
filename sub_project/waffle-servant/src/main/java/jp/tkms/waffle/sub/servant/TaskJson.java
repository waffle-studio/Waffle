package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskJson {
  public static final String EXECUTABLE = "executable";
  public static final String EXECUTABLE_NAME = "executable_name";
  public static final String COMMAND = "command";
  public static final String ARGUMENT = "argument";
  public static final String PROJECT = "project";
  public static final String WORKSPACE = "workspace";
  public static final String ENVIRONMENT = "environment";
  public static final String TIMEOUT = "timeout";
  public static final String EXEC_KEY = "exec_key";

  private JsonObject instance;

  public TaskJson(JsonObject jsonObject) {
    this.instance = jsonObject;
  }

  public TaskJson(String project, String workspace, String executableName, String executable, String command, JsonArray arguments, JsonObject environments, ExecKey execKey) {
    String modifiedCommand = null;
    JsonArray modifiedArguments = new JsonArray();
    Matcher matcher = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(command);
    while (matcher.find()) {
      String token = matcher.group();
      if (matcher.group(1) != null) {
          token = matcher.group(1);
      } else if (matcher.group(2) != null) {
        token = matcher.group(2);
      }
      if (modifiedCommand == null) {
        modifiedCommand = token;
      } else {
        modifiedArguments.add(token);
      }
    }
    for (JsonValue v : arguments) {
      modifiedArguments.add(v);
    }

    this.instance = new JsonObject();
    instance.set(PROJECT, project);
    instance.set(WORKSPACE, workspace);
    instance.set(EXECUTABLE_NAME, executableName);
    instance.set(EXECUTABLE, executable);
    instance.set(COMMAND, modifiedCommand);
    instance.set(ARGUMENT, modifiedArguments);
    instance.set(ENVIRONMENT, environments);
    instance.set(EXEC_KEY, execKey.toString());
  }

  public long getTimeout() {
    return instance.getLong(TIMEOUT, -1);
  }

  private String getStringOrThrowException(String key) throws Exception {
    String value = instance.getString(key, null);
    if (value == null) {
      throw new Exception();
    }
    return value;
  }

  public String getProject() throws Exception {
    return getStringOrThrowException(PROJECT);
  }

  public String getWorkspace() throws Exception {
    return getStringOrThrowException(WORKSPACE);
  }

  public String getExecutableName() throws Exception {
    return getStringOrThrowException(EXECUTABLE_NAME);
  }

  public String getExecutable() throws Exception {
    return getStringOrThrowException(EXECUTABLE);
  }

  public String getCommand() throws Exception {
    return getStringOrThrowException(COMMAND);
  }

  public JsonArray getArguments() {
    return instance.get(ARGUMENT).asArray();
  }

  public JsonObject getEnvironments() {
    return instance.get(ENVIRONMENT).asObject();
  }

  public ExecKey getExecKey() {
    try {
      return new ExecKey(getStringOrThrowException(EXEC_KEY));
    } catch (Exception e) {
      return new ExecKey();
    }
  }

  public void setTimeout(long timeout) {
    instance.set(TIMEOUT, timeout);
  }

  @Override
  public String toString() {
    return instance.toString();
  }
}
