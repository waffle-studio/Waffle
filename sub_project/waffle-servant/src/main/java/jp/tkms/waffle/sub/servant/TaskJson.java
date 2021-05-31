package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class TaskJson {
  public static final String EXECUTABLE_BASE = "executable_base";
  public static final String COMMAND = "command";
  public static final String ARGUMENT = "argument";
  public static final String PROJECT = "project";
  public static final String ENVIRONMENT = "environment";
  public static final String TIMEOUT = "timeout";

  private JsonObject instance;

  public TaskJson(JsonObject jsonObject) {
    this.instance = jsonObject;
  }

  public TaskJson(String project, String executableBase, String command, JsonArray arguments, JsonObject environments, JsonObject localShared) {
    this.instance = new JsonObject();
    instance.set(PROJECT, project);
    instance.set(EXECUTABLE_BASE, executableBase);
    instance.set(COMMAND, command);
    instance.set(ARGUMENT, arguments);
    instance.set(ENVIRONMENT, environments);
    instance.set(Constants.LOCAL_SHARED, localShared);
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

  public String getExecutableBase() throws Exception {
    return getStringOrThrowException(EXECUTABLE_BASE);
  }

  public String getCommand() throws Exception {
    return getStringOrThrowException(COMMAND);
  }

  public JsonArray getArguments() {
    return instance.get(ARGUMENT).asArray();
  }

  public JsonObject getLocalShared() {
    return instance.get(Constants.LOCAL_SHARED).asObject();
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
