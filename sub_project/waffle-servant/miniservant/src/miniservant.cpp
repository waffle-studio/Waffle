#include <iostream>
#include <string>
#include <fstream>
#include <csignal>
#include <stdlib.h>
#include <unistd.h>
#include "json.hpp"
#include "subprocess.hpp"
#include "dirhash.hpp"
#include "miniservant.h"

const short SHUTDOWN_TIMEOUT = 3;

std::string taskPid = "";

void termSignalHandler(int sig)
{
    exit(1);
};

void recursiveKill(std::string child_pid)
{
    auto process = subprocess::run({"ps", "--ppid", child_pid, "-o", "pid="});
    std::string buf;
    while(std::getline(std::stringstream(process.cout), buf))
        recursiveKill(buf);
    subprocess::run({"kill", "-9", child_pid});
};

void finalizeTask()
{
    if (taskPid != "")
    {
        recursiveKill(taskPid);
    }
};

void execTask(std::filesystem::path base_directory, std::filesystem::path task_json)
{
    if (!std::filesystem::is_regular_file(task_json))
    {
        std::cerr << task_json.string() << " is not found" << std::endl;
        return;
    }

    nlohmann::json task;
    try
    {
        auto stream = std::ifstream(task_json);
        task = nlohmann::json::parse(stream);
        stream.close(); // the stream will auto close in next line.
    }
    catch(const std::exception& e)
    {
        std::cerr << e.what() << std::endl;
        return;
    }
    
    std::cout << task["pi"] << std::endl;
    std::cout << task.dump() << std::endl;
    system("sleep 10");
};

int main(int argc, char* argv[]) {
    unsigned short exitcode = 1;
    auto baseDirectory = std::filesystem::absolute(std::filesystem::path("."));
    if (argc >= 1)
    {
        baseDirectory = std::filesystem::absolute(std::filesystem::path(std::string(argv[1])));
        baseDirectory.make_preferred();
        baseDirectory = std::filesystem::path(baseDirectory.lexically_normal());
    }

    if (argc >= 3 && strcmp(argv[2], "exec") == 0)
    {
        std::signal(SIGTERM, termSignalHandler);
        std::atexit(finalizeTask);
        execTask(baseDirectory, std::filesystem::path(std::string(argv[3])));
        exitcode = 0;
    }
    else if (argc >= 2 && strcmp(argv[2], "sync_hash") == 0)
    {
        miniservant::dirhash(baseDirectory, std::filesystem::absolute(std::filesystem::path("."))).save();
        exitcode = 0;
    }

    return exitcode;
};
