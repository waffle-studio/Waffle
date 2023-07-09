#include <iostream>
#include <string>
#include <fstream>
#include <csignal>
#include <stdlib.h>
#include <unistd.h>
#include "json.hpp"
#include "subprocess.hpp"
#include "dirhash.hpp"
#include "taskexec.hpp"
#include "miniservant.h"

miniservant::taskexec* executer = nullptr;

void termSignalHandler(int sig)
{
    exit(1);
};

void finalizeTask()
{
    if (executer != nullptr)
    {
        executer->shutdown();
        delete executer;
    }
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
        executer = new miniservant::taskexec(baseDirectory, std::filesystem::path(std::string(argv[3])));
        executer->execute();
        exitcode = 0;
    }
    else if (argc >= 2 && strcmp(argv[2], "sync_hash") == 0)
    {
        miniservant::dirhash(baseDirectory, std::filesystem::absolute(std::filesystem::path("."))).save();
        exitcode = 0;
    }

    return exitcode;
};
