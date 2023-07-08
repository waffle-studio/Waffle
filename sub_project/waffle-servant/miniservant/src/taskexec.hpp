#include <string>
#include <vector>
#include <thread>
#include <filesystem>
#include "json.hpp"

namespace miniservant
{
    class taskexec
    {
    public:
        taskexec(std::filesystem::path, std::filesystem::path);
        ~taskexec();
        void shutdown();
        void close();
        void execute();
        bool authorizeExecKey();
        void createRecursiveLink(std::filesystem::path, std::filesystem::path, std::filesystem::path, std::filesystem::path);

        void flagWatchdogThreadFunc();

        // private:
        std::filesystem::path baseDirectory;
        std::filesystem::path taskJsonPath;
        std::filesystem::path taskDirectory;
        std::filesystem::path executableBaseDirectory;
        std::string projectName;
        std::string workspaceName;
        std::string executableName;
        std::string command;
        nlohmann::json argumentList;
        nlohmann::json environmentMap;
        long timeout;
        std::string execKey;

        std::thread* flagWatchdogThread = nullptr;
        bool isClosed = FALSE;
    };
}