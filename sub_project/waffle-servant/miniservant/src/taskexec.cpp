#include <iostream>
#include <fstream>
#include <thread>
#include <chrono>
#include <unistd.h>
#include "subprocess.hpp"
#include "dirhash.hpp"
#include "taskexec.hpp"

//FlagWatchdog flagWatchdog;

namespace miniservant
{
  const short SHUTDOWN_TIMEOUT = 3;
  const short DIRECTORY_SYNCHRONIZATION_TIMEOUT = 300;

  void recursiveKill(std::string child_pid)
  {
    auto process = subprocess::run({"ps", "--ppid", child_pid, "-o", "pid="});
    std::string buf;
    while (std::getline(std::stringstream(process.cout), buf))
      recursiveKill(buf);
    subprocess::run({"kill", "-9", child_pid});
  };

  void createFile(std::filesystem::path path, std::string contents)
  {
    auto stream = std::ofstream(path);
    stream << contents;
    stream.close();
  }

  inline std::filesystem::path _path_normalize(std::filesystem::path path)
  {
    path.make_preferred();
    return std::filesystem::path(path.lexically_normal());
  };

  taskexec::taskexec(std::filesystem::path base_directory, std::filesystem::path task_json_path)
  {
    this->baseDirectory = _path_normalize(base_directory);
    this->taskJsonPath = _path_normalize(task_json_path);
    this->taskDirectory = this->taskJsonPath.parent_path();
    this->environmentList = std::vector<std::string>();

    nlohmann::json taskJson;
    try
    {
        auto stream = std::ifstream(task_json_path);
        taskJson = nlohmann::json::parse(stream);
        stream.close(); // the stream will auto close in next line.
    }
    catch(const std::exception& e)
    {
        std::cerr << e.what() << std::endl;
        return;
    }

    auto taskJsonExecutable = taskJson["executable"];
    if (taskJsonExecutable.is_null())
      this->executableBaseDirectory = _path_normalize(this->baseDirectory / taskJsonExecutable / "BASE");
    else
      this->executableBaseDirectory = std::filesystem::path("/");

    projectName = taskJson["project"];
    workspaceName = taskJson["workspace"];
    executableName = taskJson["executable"];
    command = taskJson["command"];
    argumentList = taskJson["argument"];
    environmentMap = taskJson["environment"];
    timeout = taskJson["timeout"];
    execKey = taskJson["exec_key"];

    pid = "";
  }

  void taskexec::shutdown()
  {
    if (this->pid == "")
      return;
    for (int sec = 0; sec < SHUTDOWN_TIMEOUT; sec += 1)
    {
      if (subprocess::run({"kill", "-0", this->pid}).returncode == 1)
        return;
      std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    recursiveKill(this->pid);
  }

  void taskexec::close()
  {
    /*
    if (flagWatchdog != null)
    {
      flagWatchdog.close();
    }
    */

    auto stream = std::ofstream(this->baseDirectory / ".NOTIFIER");
    auto time = duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    stream << this->execKey + std::to_string(time);
    stream.close();
  }

  void taskexec::execute()
  {
    try {
      if (!authorizeExecKey()) {
        return;
      }

      //flagWatchdog = new FlagWatchdog();
      //flagWatchdog.start();

      dirhash directoryHash = dirhash(baseDirectory, this->taskDirectory);
      directoryHash.waitToMatch(DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      if (executableBaseDirectory != std::filesystem::path("/"))
      {
        dirhash(baseDirectory, executableBaseDirectory.parent_path()).waitToMatch(DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      }

      std::filesystem::path executingBaseDirectory = taskDirectory / "BASE";
      std::filesystem::path stdoutPath = taskDirectory / "STDOUT.txt";
      std::filesystem::path stderrPath = taskDirectory / "STDERR.txt";
      std::filesystem::path eventFilePath = taskDirectory / "EVENT.bin";
      std::filesystem::path statusFilePath = taskDirectory / "EXET_STATUS.log";

      char* waffleSlotIndex = std::getenv("WAFFLE_SLOT_INDEX");
      if (waffleSlotIndex != NULL)
        subprocess::cenv["WAFFLE_SLOT_INDEX"] = std::string(waffleSlotIndex);

      addEnvironment("PATH", (this->baseDirectory / "bin").string() + ":" + std::string(std::getenv("PATH")));
      addEnvironment("WAFFLE_BASE", executingBaseDirectory.string());
      addEnvironment("WAFFLE_TASK_JSONFILE", taskJsonPath.string());
      addEnvironment("WAFFLE_BATCH_WORKING_DIR", taskDirectory.string());
      addEnvironment("WAFFLE_WORKING_DIR", executingBaseDirectory.string());

      createFile(stdoutPath, "");
      createFile(stderrPath, "");
      createFile(eventFilePath, "");

      // write a status file
      createFile(statusFilePath, "-2");

      std::filesystem::create_directories(executingBaseDirectory);

      if (executableBaseDirectory != std::filesystem::path("/"))
      {
        // try to change permission of the command
        subprocess::run
        subprocess::run({"sh", "-c", "chmod a+x '" + command + "' >/dev/null 2>&1"})
        Runtime.getRuntime().exec(new String[],
          getEnvironments(), executableBaseDirectory.toFile()).waitFor();

        // prepare local shared directory path
        Path projectLocalSharedDirectory = baseDirectory.resolve(Constants.PROJECT).resolve(projectName)
          .resolve(Constants.LOCAL_SHARED).resolve(executableName).normalize();
        Path workspaceLocalSharedDirectory = baseDirectory.resolve(Constants.PROJECT).resolve(projectName)
          .resolve(Constants.WORKSPACE).resolve(workspaceName)
          .resolve(Constants.LOCAL_SHARED).resolve(executableName).normalize();

        // create link of executable entities
        createRecursiveLink(executableBaseDirectory, executingBaseDirectory,
          projectLocalSharedDirectory, workspaceLocalSharedDirectory);
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
    } finally {
      close();
    }
  }

  private void createRecursiveLink(Path source, Path destination, Path projectLocalShared, Path workspaceLocalShared) throws IOException {
    if (Files.isDirectory(source)) {
      Files.createDirectories(destination);
    }
    try (Stream<Path> stream = Files.list(source)) {
      stream.forEach(path -> {
        try {
          Path localShared = projectLocalShared;

          switch (LocalSharedFlag.getFlag(path).getLevel()) {
            case None:
              if (Files.isDirectory(path)) {
                createRecursiveLink(path, destination.resolve(path.getFileName()),
                  projectLocalShared.resolve(path.getFileName()), workspaceLocalShared.resolve(path.getFileName()));
              } else {
                Files.createSymbolicLink(destination.resolve(path.getFileName()), path);
              }
              break;
            case Run:
              if (Files.isDirectory(path)) {
                recursiveMerge(path, destination.resolve(path.getFileName()));
              } else {
                Files.copy(path, destination.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
              }
              break;
            case Workspace: // replace localShared from project's.
              localShared = workspaceLocalShared;
            case Project: // localShared is projectLocalShared when Project case
              Path sharedPath = localShared.resolve(path.getFileName());
              if (Files.isDirectory(path)) {
                recursiveMerge(path, sharedPath);
              } else {
                merge(path, sharedPath);
              }
              Files.createSymbolicLink(destination.resolve(path.getFileName()), sharedPath);
              break;
          }
        } catch (IOException e) {
          e.printStackTrace();
          try {
            Files.writeString(Paths.get("/tmp/err"), e.toString());
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      });
    }
  }

  private void recursiveMerge(Path source, Path destination) throws IOException {
    if (Files.isDirectory(source)) {
      Files.createDirectories(destination);
    }
    try (Stream<Path> stream = Files.list(source)) {
      stream.forEach(path -> {
        try {
          if (Files.isDirectory(path)) {
            recursiveMerge(path, destination.resolve(path.getFileName()));
          } else {
            merge(path, destination.resolve(path.getFileName()));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }
  }

  private void merge(Path source, Path destination) {
    if (!Files.exists(destination)) {
      try {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void addEnvironment(String name, String value) {
    try {
      //Process process = Runtime.getRuntime().exec(new String[]{"echo", "-n", value},
        //getEnvironments(), executableBaseDirectory.toFile());
      Path workingDirectory = Paths.get(".");
      if (executableBaseDirectory != null) {
        workingDirectory = executableBaseDirectory;
      }
      Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "echo -n \"" + value + "\""},
        getEnvironments(), workingDirectory.toFile());
      InputStream inputStream = process.getInputStream();
      process.waitFor();
      value = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    environmentList.add(name + '=' + value);
  }

  private String[] getEnvironments() {
    return environmentList.toArray(new String[environmentList.size()]);
  }

  private class FlagWatchdog extends Thread {
    Path flagPath;
    boolean interrupted;

    public FlagWatchdog() {
      this.interrupted = false;
      this.flagPath = getTaskDirectory().resolve(Constants.ALIVE);

      try {
        Files.createFile(flagPath);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        flagPath.toFile().deleteOnExit();
      }
    }

    @Override
    public void run() {
      while (Files.exists(flagPath) && !interrupted) {
        try {
          TimeUnit.SECONDS.sleep(Constants.WATCHDOG_INTERVAL);
        } catch (InterruptedException e) {
          continue;
        }
      }

      if (!interrupted) {
        System.err.println("The task will be killed because missing " + flagPath);
        recursiveKill(String.valueOf(getPid()));
        System.exit(1);
      }
      return;
    }

    public void close() {
      interrupted = true;
      interrupt();
    }
  }
}