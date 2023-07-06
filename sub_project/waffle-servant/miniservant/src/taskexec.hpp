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
        void shutdown();
        void close();
        void execute();
        bool authorizeExecKey();

        // private:
        std::filesystem::path baseDirectory;
        std::filesystem::path taskJsonPath;
        std::filesystem::path taskDirectory;
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