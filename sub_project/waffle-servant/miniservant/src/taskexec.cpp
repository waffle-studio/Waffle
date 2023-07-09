#include <iostream>
#include <fstream>
#include <chrono>
#include <unistd.h>
#include "subprocess.hpp"
#include "dirhash.hpp"
#include "outproc.hpp"
#include "taskexec.hpp"

//FlagWatchdog flagWatchdog;

namespace miniservant
{
  const short WATCHDOG_INTERVAL = 2;
  const short SHUTDOWN_TIMEOUT = 3;
  const short DIRECTORY_SYNCHRONIZATION_TIMEOUT = 300;
  const std::string WAFFLE_LOCAL_SHARED(".WAFFLE_LOCAL_SHARED");


  void recursiveKill(std::string child_pid)
  {
    auto process = subprocess::run({"ps", "--ppid", child_pid, "-o", "pid="}, {.cout = subprocess::PipeOption::pipe});
    std::string buf = process.cout;
    auto ss = std::stringstream(process.cout);
    while (buf.size() > 0 && std::getline(ss, buf))
    {
      recursiveKill(buf);
    }
    if (child_pid != std::to_string(getpid()))
      subprocess::run({"kill", "-9", child_pid});
  };

  void createFile(std::filesystem::path path, std::string contents)
  {
    auto stream = std::ofstream(path);
    stream << contents;
    stream.close();
  }

  void flagWatchdogThreadFunc(bool* is_closed, std::filesystem::path flag_path)
  {
    createFile(flag_path, "");
    while (std::filesystem::exists(flag_path) && *is_closed)
      std::this_thread::sleep_for(std::chrono::seconds(1));

    if (!std::filesystem::exists(flag_path))
    {
      std::cerr << "The task will be killed because missing " << flag_path.string() << std::endl;
      recursiveKill(std::to_string(getpid()));
      exit(1);
    }
    else
      std::filesystem::remove(flag_path);
  }

  inline std::filesystem::path *_new_normalized_path(std::filesystem::path path)
  {
    auto p = std::filesystem::path(path);
    p.make_preferred();
    return new std::filesystem::path(p.lexically_normal());
  };

  inline nlohmann::json _default(nlohmann::json v, nlohmann::json d)
  {
    if (v.is_null())
      return d[""];
    return v;
  };

  taskexec::taskexec(std::filesystem::path base_directory, std::filesystem::path task_json_path)
  {
    this->baseDirectory = _new_normalized_path(base_directory);
    this->taskJsonPath = _new_normalized_path(task_json_path);
    this->taskDirectory = new std::filesystem::path(this->taskJsonPath->parent_path());

    nlohmann::json taskJson;
    try
    {
        auto stream = std::ifstream(*this->taskJsonPath);
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
      this->executableBaseDirectory = nullptr;
    else
      this->executableBaseDirectory = new std::filesystem::path((*this->baseDirectory / taskJsonExecutable / "BASE"));

    projectName = taskJson["project"];
    workspaceName = taskJson["workspace"];
    executableName = taskJson["executable"];
    command = taskJson["command"];
    argumentList = taskJson["argument"];
    environmentMap = taskJson["environment"];
    timeout = _default(taskJson["timeout"], nlohmann::json::parse("{\"\": -1}"));
    execKey = taskJson["exec_key"];
  }

  taskexec::~taskexec()
  {
    close();
    delete baseDirectory;
    delete taskJsonPath;
    delete taskDirectory;
    if (executableBaseDirectory != nullptr)
      delete executableBaseDirectory;
  }

  void taskexec::shutdown()
  {
    for (int sec = 0; sec < SHUTDOWN_TIMEOUT; sec += 1)
    {
      auto process = subprocess::run({"ps", "--ppid", std::to_string(getpid()), "-o", "pid="}, {.cout = subprocess::PipeOption::pipe});
      std::string buf = process.cout;
      long count = 0;
      auto ss = std::stringstream(process.cout);
      while (std::getline(ss, buf))
        count += 1;
      if (count <= 1)
        return;
      std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    recursiveKill(std::to_string(getpid()));
  }

  void taskexec::close()
  {
    if (!this->isClosed)
    {
      this->isClosed = true;

      auto stream = std::ofstream(*this->baseDirectory / ".NOTIFIER");
      auto time = duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
      stream << this->execKey + std::to_string(time);
      stream.close();
    }
  }

  void taskexec::execute()
  {
    if (command == "#PODTASK")
    {
      subprocess::run({"echo", "PODTASK"});
      exit(126);
    }

    try {
      if (!authorizeExecKey()) {
        return;
      }

      auto flagPath = *this->taskDirectory / ".ALIVE";
      std::thread(miniservant::flagWatchdogThreadFunc, &this->isClosed, flagPath).detach();

      dirhash directoryHash = dirhash(*this->baseDirectory, *this->taskDirectory);
      directoryHash.waitToMatch(DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      if (executableBaseDirectory != nullptr)
      {
        dirhash(*this->baseDirectory, executableBaseDirectory->parent_path()).waitToMatch(DIRECTORY_SYNCHRONIZATION_TIMEOUT);
      }

      std::filesystem::path executingBaseDirectory = *this->taskDirectory / "BASE";
      std::filesystem::path stdoutPath = *this->taskDirectory / "STDOUT.txt";
      std::filesystem::path stderrPath = *this->taskDirectory / "STDERR.txt";
      std::filesystem::path eventFilePath = *this->taskDirectory / "EVENT.bin";
      std::filesystem::path statusFilePath = *this->taskDirectory / "EXIT_STATUS.log";

      char* waffleSlotIndex = std::getenv("WAFFLE_SLOT_INDEX");
      if (waffleSlotIndex != NULL)
        subprocess::cenv["WAFFLE_SLOT_INDEX"] = std::string(waffleSlotIndex);

      subprocess::cenv["PATH"] = (*this->baseDirectory / "bin").string() + ":" + std::string(std::getenv("PATH"));
      subprocess::cenv["WAFFLE_BASE"] = executingBaseDirectory.string();
      subprocess::cenv["WAFFLE_TASK_JSONFILE"] = this->taskJsonPath->string();
      subprocess::cenv["WAFFLE_BATCH_WORKING_DIR"] = this->taskDirectory->string();
      subprocess::cenv["WAFFLE_WORKING_DIR"] = executingBaseDirectory.string();

      createFile(stdoutPath, "");
      createFile(stderrPath, "");
      createFile(eventFilePath, "");

      // write a status file
      createFile(statusFilePath, "-2");

      std::filesystem::create_directories(executingBaseDirectory);

      // load custom environments
      for (auto &[key, value] : environmentMap.items())
      {
        subprocess::cenv[key] = std::string(value); // dump()?
      }

      if (executableBaseDirectory != nullptr)
      {
        // try to change permission of the command
        subprocess::run({"sh", "-c", "chmod a+x '" + command + "' >/dev/null 2>&1"}, {.cwd = executableBaseDirectory->string()});

        // prepare local shared directory path
        auto projectLocalSharedDirectory = *this->baseDirectory / "PROJECT" / projectName / "LOCAL_SHARED" / executableName;
        auto workspaceLocalSharedDirectory = *this->baseDirectory / "PROJECT" / projectName / "WORKSPACE" / workspaceName / "LOCAL_SHARED" / executableName;

        // create link of executable entities
        createRecursiveLink(*executableBaseDirectory, executingBaseDirectory, projectLocalSharedDirectory, workspaceLocalSharedDirectory);
      }

      int exitValue = 0;

      // BEGIN of main command executing
      auto commandArray = std::vector<std::string>();
      // commandArray.addAll(Arrays.asList(command.split("\\s")));
      commandArray.push_back(command);
      for (auto &value : argumentList)
      {
        commandArray.push_back((std::string)value);
      }

      chdir(executingBaseDirectory.c_str());
      auto process = subprocess::RunBuilder(commandArray).cwd(executingBaseDirectory.string()).cerr(subprocess::PipeOption::pipe).cout(subprocess::PipeOption::pipe).popen();

      outproc outProcessor = outproc(&process.cout, stdoutPath, *this->baseDirectory, eventFilePath);
      outproc errProcessor = outproc(&process.cerr, stderrPath, *this->baseDirectory, eventFilePath);
      outProcessor.start();
      errProcessor.start();

      if (this->timeout < 0)
        process.wait();
      else
        try
        {
          process.wait(timeout);
        }
        catch (const std::exception &e)
        {
          process.kill();
        }

      outProcessor.join();
      errProcessor.join();
      // END of main command executing

      exitValue = process.returncode;

      // write a status file
      createFile(statusFilePath, std::to_string(exitValue));

      // update hash file
      directoryHash.update();
    }
    catch (const std::exception &e)
    {
      std::cerr << e.what();
    }
    close();
  }

  bool taskexec::authorizeExecKey()
  {
    auto path = *this->taskDirectory / ".EXEC_KEY";
    if (!std::filesystem::exists(path))
      return false;
    auto stream = std::ifstream(path);
    auto size = std::filesystem::file_size(path);
    char data[size + 1] = {0};
    stream.read(reinterpret_cast<char *>(data), size);
    stream.close();
    return std::strcmp(this->execKey.c_str(), data) == 0;
  }

  LocalSharedFlag taskexec::getLocalSharedFlag(std::filesystem::path path)
  {
    auto flagPath = path.parent_path() / (WAFFLE_LOCAL_SHARED + "." + path.filename().string()); 
    if (std::filesystem::is_directory(path))
    {
      flagPath = path.parent_path() / (WAFFLE_LOCAL_SHARED + "." + path.filename().string()); 
      if (!std::filesystem::is_regular_file(flagPath))
        flagPath = path / WAFFLE_LOCAL_SHARED;
    }

    if (std::filesystem::is_regular_file(flagPath))
    {
      auto stream = std::ifstream(flagPath);
      if (std::filesystem::file_size(flagPath) <= 0)
        return LocalSharedFlag::None;
      char data[1];
      stream.read(reinterpret_cast<char *>(data), 1);
      switch (data[0])
      {
        case 'w':
        case 'W':
          return LocalSharedFlag::Workspace;
        case 'p':
        case 'P':
          return LocalSharedFlag::Project;
      }
      return LocalSharedFlag::Run;
    }

    return LocalSharedFlag::None;
  }

  void taskexec::createRecursiveLink(std::filesystem::path source, std::filesystem::path destination, std::filesystem::path projectLocalShared, std::filesystem::path workspaceLocalShared)
  {
    if (std::filesystem::is_directory(source))
      std::filesystem::create_directories(destination);

    for (const auto &entry : std::filesystem::directory_iterator(source))
    {
      auto path = entry.path();
      if (path.filename().string() == "." || path.filename().string() == "..")
        continue;
      auto localShared = projectLocalShared;
      switch (getLocalSharedFlag(path))
      {
      case None:
        if (std::filesystem::is_directory(path))
          createRecursiveLink(path, destination / path.filename(),
                              projectLocalShared / path.filename(), workspaceLocalShared / path.filename());
        else
        {
          if (std::filesystem::exists(destination / path.filename()))
            std::filesystem::remove(destination / path.filename());
          std::filesystem::create_symlink(path, destination / path.filename());
        }
        break;
      case Run:
        if (std::filesystem::is_directory(path))
          recursiveMerge(path, destination / path.filename());
        else
          std::filesystem::copy(path, destination / path.filename(), std::filesystem::copy_options::overwrite_existing);
        break;
      case Workspace: // replace localShared from project's.
        localShared = workspaceLocalShared;
      case Project: // localShared is projectLocalShared when Project case
        auto sharedPath = localShared / path.filename();
        if (std::filesystem::is_directory(path))
          recursiveMerge(path, sharedPath);
        else
          merge(path, sharedPath);
        std::filesystem::create_symlink(sharedPath, destination / path.filename());
        break;
      }
    }
  }

  void taskexec::recursiveMerge(std::filesystem::path source, std::filesystem::path destination)
  {
    if (std::filesystem::is_directory(source))
      std::filesystem::create_directories(destination);
    for (const auto &entry : std::filesystem::directory_iterator(source))
    {
      auto path = entry.path();
      if (path.filename().string() == "." || path.filename().string() == "..")
        continue;
      if (std::filesystem::is_directory(path))
        recursiveMerge(path, destination / path.filename());
      else
        merge(path, destination / path.filename());
    }
  }

  void taskexec::merge(std::filesystem::path source, std::filesystem::path destination)
  {
    if (!std::filesystem::exists(destination))
      std::filesystem::copy(source, destination, std::filesystem::copy_options::overwrite_existing);
  }

  std::string taskexec::extractEnvValue(std::string value)
  {
    auto workingDirectory = std::filesystem::path(".");
    if (this->executableBaseDirectory != nullptr)
    {
      workingDirectory = *executableBaseDirectory;
    }
    auto process = subprocess::run({"echo", "-n", value}, {.cwd = workingDirectory.string()});
    return process.cout;
  }
}