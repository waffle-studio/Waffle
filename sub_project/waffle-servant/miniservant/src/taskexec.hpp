#include <string>
#include <vector>
#include <filesystem>
#include "json.hpp"

namespace miniservant
{
    class taskexec
    {
    public:
        taskexec(std::filesystem::path, std::filesystem::path);

        // private:
        std::filesystem::path baseDirectory;
        std::filesystem::path taskJsonPath;
        std::vector<std::string> environmentList;
        std::filesystem::path executableBaseDirectory;
        std::string projectName;
        std::string workspaceName;
        std::string executableName;
        std::string command;
        nlohmann::json argumentList;
        nlohmann::json environmentMap;
        long timeout;
        std::string pid;
        std::string execKey;
    };
}