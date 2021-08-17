package jp.tkms.waffle.sub.servant;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jp.tkms.waffle.sub.servant.pod.PodTask;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
  private long pid;

  public TaskExecutor(Path baseDirectory, Path taskJsonPath) throws Exception {
    this.baseDirectory = baseDirectory;

    if (!taskJsonPath.isAbsolute()) {
      taskJsonPath = baseDirectory.resolve(taskJsonPath);
    }
    this.taskDirectory = taskJsonPath.getParent().normalize();

    this.environmentList = new ArrayList<>();

    TaskJson taskJson = new TaskJson(Json.parse(new FileReader(taskJsonPath.toFile())).asObject());
    try {
      executableBaseDirectory = baseDirectory.resolve(taskJson.getExecutable()).resolve(Constants.BASE).normalize();
    } catch (Exception e) {
      executableBaseDirectory = null;
    }
    projectName = taskJson.getProject();
    command = taskJson.getCommand();
    argumentList = taskJson.getArguments();
    localSharedMap = taskJson.getLocalShared();
    environmentMap = taskJson.getEnvironments();
    timeout = taskJson.getTimeout();

    pid = Long.valueOf(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  }

  private void setPid(long pid) {
    this.pid = pid;
  }

  public long getPid() {
    return pid;
  }

  public void shutdown() {

  }

  public void execute() {
    try {
      DirectoryHash directoryHash = new DirectoryHash(baseDirectory, taskDirectory);
      directoryHash.waitToMatch(Constants.DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      if (executableBaseDirectory != null) {
        new DirectoryHash(baseDirectory, executableBaseDirectory.getParent()).waitToMatch(Constants.DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      }

      Path executingBaseDirectory = taskDirectory.resolve(Constants.BASE).normalize();
      Path localSharedDirectory = baseDirectory.resolve(Constants.LOCAL_SHARED).resolve(projectName).normalize();
      Path stdoutPath = taskDirectory.resolve(Constants.STDOUT_FILE);
      Path stderrPath = taskDirectory.resolve(Constants.STDERR_FILE);
      Path eventFilePath = taskDirectory.resolve(Constants.EVENT_FILE);
      Path statusFilePath = taskDirectory.resolve(Constants.EXIT_STATUS_FILE);

      if (System.getenv().containsKey(Constants.WAFFLE_SLOT_INDEX)) {
        addEnvironment(Constants.WAFFLE_SLOT_INDEX, System.getenv().get(Constants.WAFFLE_SLOT_INDEX));
      }

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

      // write a status file
      try {
        FileWriter writer = new FileWriter(statusFilePath.toFile(), StandardCharsets.UTF_8, false);
        writer.write("-2");
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      Files.createDirectories(executingBaseDirectory);

      if (executableBaseDirectory != null) {
        // try to change permission of the command
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod a+x '" + command + "' >/dev/null 2>&1"},
          getEnvironments(), executableBaseDirectory.toFile()).waitFor();

        // create link of executable entities
        createRecursiveLink(executableBaseDirectory, executingBaseDirectory);
      }

      // load custom environments
      for (JsonObject.Member member : environmentMap) {
        addEnvironment(member.getName(), member.getValue().asString());
      }

      int exitValue = 0;

      if (command.equals(PodTask.PODTASK)) {
        try {
          PodTask podTask = new PodTask(argumentList);
          podTask.run();
        } catch (Exception e) {
          exitValue = 1;
        }
      } else {
        // pre-process for local shared
        addEnvironment("WAFFLE_LOCAL_SHARED", localSharedDirectory.toString());
        Files.createDirectories(localSharedDirectory);
        for (JsonObject.Member member : localSharedMap) {
          String key = member.getName();
          String remote = member.getValue().asString();
          Runtime.getRuntime().exec(new String[]{"sh", "-c", "mkdir -p `dirname \"" + remote + "\"`;if [ -e \"${WAFFLE_LOCAL_SHARED}/" + key + "\" ]; then ln -fs \"${WAFFLE_LOCAL_SHARED}/" + key + "\" \"" + remote + "\"; else echo \"" + key + "\" >> \"${WAFFLE_BATCH_WORKING_DIR}/non_prepared_local_shared.txt\"; fi"},
            getEnvironments(), executingBaseDirectory.toFile()).waitFor();
        }

        // BEGIN of main command executing
        ArrayList<String> commandArray = new ArrayList<>();
        commandArray.addAll(Arrays.asList(command.split("\\s")));
        for (JsonValue value : argumentList) {
          commandArray.add(value.isString() ? value.asString() : value.toString());
        }

        Process process = Runtime.getRuntime().exec(commandArray.toArray(new String[commandArray.size()]),
          getEnvironments(), executingBaseDirectory.toFile());
        setPid(process.pid());

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

        exitValue = process.exitValue();
      }

      // write a status file
      try {
        FileWriter writer = new FileWriter(statusFilePath.toFile(), StandardCharsets.UTF_8, false);
        writer.write(String.valueOf(exitValue));
        writer.flush();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // update hash file
      directoryHash.update();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void createRecursiveLink(Path source, Path destination) throws IOException {
    if (Files.isDirectory(source)) {
      Files.createDirectories(destination);
    }
    try (Stream<Path> stream = Files.list(source)) {
      stream.forEach(path -> {
        try {
          if (Files.isDirectory(path)) {
            createRecursiveLink(path, destination.resolve(path.getFileName()));
          } else {
            Files.createLink(destination.resolve(path.getFileName()), path);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }
  }

  public void addEnvironment(String name, String value) {
    environmentList.add(name + '=' + value);
  }

  private String[] getEnvironments() {
    return environmentList.toArray(new String[environmentList.size()]);
  }
}
