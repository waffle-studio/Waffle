#include <iostream>
#include <string>
#include <fstream>
#include <stdlib.h>
#include <unistd.h>
#include "json.hpp"
#include "dirhash.hpp"
#include "miniservant.h"

using json = nlohmann::json;

void execTask(std::filesystem::path base_directory, std::filesystem::path task_json)
{
    if (!std::filesystem::is_regular_file(task_json))
        return;

    json task;
    try
    {
        auto stream = std::ifstream(task_json);
        task = json::parse(stream);
        stream.close();
    }
    catch(const std::exception& e)
    {
        task = json::parse("{}");
        std::cerr << e.what() << std::endl;
    }
    
    std::cout << task["pi"] << std::endl;
    std::cout << task.dump() << std::endl;
    //system("sleep 5");
}

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
        execTask(baseDirectory, std::filesystem::path(std::string(argv[3])));
        exitcode = 0;
    }
    else if (argc >= 2 && strcmp(argv[2], "sync_hash") == 0)
    {
        miniservant::dirhash(baseDirectory, std::filesystem::absolute(std::filesystem::path("."))).save();
        exitcode = 0;
    }

    return exitcode;
}
