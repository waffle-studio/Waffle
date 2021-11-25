package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class TaskJson {
  public static final String EXECUTABLE = "executable";
  public static final String EXECUTABLE_NAME = "executable_name";
  public static final String COMMAND = "command";
  public static final String ARGUMENT = "argument";
  public static final String PROJECT = "project";
  public static final String WORKSPACE = "workspace";
  public static final String ENVIRONMENT = "environment";
  public static final String TIMEOUT = "timeout";

  private JsonObject instance;

  public TaskJson(JsonObject jsonObject) {
    this.instance = jsonObject;
  }

  public TaskJson(String project, String workspace, String executableName, String executable, String command, JsonArray arguments, JsonObject environments) {
    this.instance = new JsonObject();
    instance.set(PROJECT, project);
    instance.set(WORKSPACE, workspace);
    instance.set(EXECUTABLE_NAME, executableName);
    instance.set(EXECUTABLE, executable);
    instance.set(COMMAND, command);
    instance.set(ARGUMENT, arguments);
    instance.set(ENVIRONMENT, environments);
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

  public void setTimeout(long timeout) {
    instance.set(TIMEOUT, timeout);
  }

  @Override
  public String toString() {
    return instance.toString();
  }
}
