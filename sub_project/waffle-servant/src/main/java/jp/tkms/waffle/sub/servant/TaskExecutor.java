package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TaskExecutor {

  ArrayList<String> environmentList;
  Path baseDirectory;
  Path taskDirectory;
  Path executableBaseDirectory;
  String projectName;
  String command;
  JsonArray argumentList;
  JsonObject localSharedMap;
  JsonObject environmentMap;
  long timeout;

  public TaskExecutor(Path baseDirectory, Path taskJsonPath) throws Exception {
    this.baseDirectory = baseDirectory;

    if (!taskJsonPath.isAbsolute()) {
      taskJsonPath = baseDirectory.resolve(taskJsonPath);
    }
    this.taskDirectory = taskJsonPath.getParent().normalize();

    this.environmentList = new ArrayList<>();

    JsonObject taskJson = Json.parse(new FileReader(taskJsonPath.toFile())).asObject();
    String jsonValue = taskJson.getString(Constants.EXECUTABLE_BASE, null);
    if (jsonValue == null) {
      throw new Exception();
    }
    executableBaseDirectory = baseDirectory.resolve(jsonValue).normalize();

    projectName = taskJson.getString(Constants.PROJECT, null);
    if (projectName == null) {
      throw new Exception();
    }

    command = taskJson.getString(Constants.COMMAND, null);
    if (command == null) {
      throw new Exception();
    }

    argumentList = taskJson.get(Constants.ARGUMENT).asArray();

    localSharedMap = taskJson.get(Constants.LOCAL_SHARED).asObject();

    environmentMap = taskJson.get(Constants.ENVIRONMENT).asObject();

    timeout = taskJson.getLong(Constants.TIMEOUT, -1);
  }

  public void execute() {
    try {
      Path executingBaseDirectory = taskDirectory.resolve(Constants.BASE).normalize();
      Path localSharedDirectory = baseDirectory.resolve(Constants.LOCAL_SHARED).resolve(projectName).normalize();
      Path stdoutPath = taskDirectory.resolve(Constants.STDOUT_FILE);
      Path stderrPath = taskDirectory.resolve(Constants.STDERR_FILE);
      Path eventFilePath = taskDirectory.resolve(Constants.EVENT_FILE);
      Path statusFilePath = taskDirectory.resolve(Constants.EXIT_STATUS_FILE);

      addEnvironment("WAFFLE_BASE", executingBaseDirectory.toString());
      addEnvironment("WAFFLE_BATCH_WORKING_DIR", taskDirectory.toString());
      addEnvironment("WAFFLE_WORKING_DIR", executingBaseDirectory.toString());

      try {
        Files.createFile(stdoutPath);
        Files.createFile(stderrPath);
        Files.createFile(eventFilePath);
      } catch (IOException e) {
        // NOP
      }

      Files.createDirectories(executingBaseDirectory);

      // try to change permission of the command
      Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod a+x '" + command + "' >/dev/null 2>&1"},
        getEnvironments(), executableBaseDirectory.toFile()).waitFor();

      // create link of executable entities
      Runtime.getRuntime().exec("find . -type d | xargs -n 1 -I{1} sh -c 'mkdir -p \"${WAFFLE_WORKING_DIR}/{1}\";find {1} -maxdepth 1 -type f | xargs -n 1 -I{2} ln -s \"`pwd`/{2}\" \"${WAFFLE_WORKING_DIR}/{1}/\"'",
        getEnvironments(), executableBaseDirectory.toFile()).waitFor();

      // pre-process for local shared
      addEnvironment("WAFFLE_LOCAL_SHARED", localSharedDirectory.toString());
      Files.createDirectories(localSharedDirectory);
      for (JsonObject.Member member : localSharedMap) {
        String key = member.getName();
        String remote = member.getValue().asString();
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "mkdir -p `dirname \"" + remote + "\"`;if [ -e \"${WAFFLE_LOCAL_SHARED}/" + key + "\" ]; then ln -fs \"${WAFFLE_LOCAL_SHARED}/" + key + "\" \"" + remote + "\"; else echo \"" + key + "\" >> \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; fi"},
          getEnvironments(), executingBaseDirectory.toFile()).waitFor();
      }

      // load custom environments
      for (JsonObject.Member member : environmentMap) {
        addEnvironment(member.getName(), member.getValue().asString());
      }

      // BEGIN of main command executing
      ArrayList<String> commandArray = new ArrayList<>();
      commandArray.addAll(Arrays.asList(command.split("\\s")));
      for (JsonValue value : argumentList) {
        commandArray.add(value.isString() ? value.asString() : value.toString());
      }

      Process process = Runtime.getRuntime().exec(commandArray.toArray(new String[commandArray.size()]),
        getEnvironments(), executingBaseDirectory.toFile());

      OutputProcessor outProcessor =
        new OutputProcessor(process.getInputStream(), stdoutPath, new EventRecorder(baseDirectory, eventFilePath));
      OutputProcessor errProcessor =
        new OutputProcessor(process.getErrorStream(), stderrPath, new EventRecorder(baseDirectory, eventFilePath));
      outProcessor.start();
      errProcessor.start();

      if (timeout < 0) {
        process.waitFor();
      } else {
        process.waitFor(timeout, TimeUnit.SECONDS);
      }

      outProcessor.join();
      errProcessor.join();
      // END of main command executing

      // post-process of local shared
      for (JsonObject.Member member : localSharedMap) {
        String key = member.getName();
        String remote = member.getValue().asString();
        Runtime.getRuntime().exec(
          new String[]{"sh", "-c", "if grep \"^" + key + "$\" \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; then mv \"" + remote + "\" \"${WAFFLE_LOCAL_SHARED}/" + key + "\"; ln -fs \"${WAFFLE_LOCAL_SHARED}/"  + key + "\" \"" + remote + "\" ;fi"},
          getEnvironments(), executingBaseDirectory.toFile()).waitFor();
      }

      // write a status file
      try {
        FileWriter writer = new FileWriter(statusFilePath.toFile(), StandardCharsets.UTF_8, false);
        writer.write("" + process.exitValue());
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void addEnvironment(String name, String value) {
    environmentList.add(name + '=' + value);
  }

  private String[] getEnvironments() {
    return environmentList.toArray(new String[environmentList.size()]);
  }
}
