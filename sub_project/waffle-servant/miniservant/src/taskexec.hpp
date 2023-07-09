#include <string>
#include <vector>
#include <thread>
#include <filesystem>
#include "json.hpp"

namespace miniservant
{
    enum LocalSharedFlag {
        None, Run, Workspace, Project
    };

    class taskexec
    {
    public:
        taskexec(std::filesystem::path*, std::filesystem::path*);
        ~taskexec();
        void shutdown();
        void close();
        void execute();
        bool authorizeExecKey();
        LocalSharedFlag getLocalSharedFlag(std::filesystem::path);
        void createRecursiveLink(std::filesystem::path, std::filesystem::path, std::filesystem::path, std::filesystem::path);
        void recursiveMerge(std::filesystem::path, std::filesystem::path);
        void merge(std::filesystem::path, std::filesystem::path);
        std::string extractEnvValue(std::string);

        // private:
        std::filesystem::path* baseDirectory;
        std::filesystem::path* taskJsonPath;
        std::filesystem::path* taskDirectory;
        std::filesystem::path* executableBaseDirectory;
        std::string projectName;
        std::string workspaceName;
        std::string executableName;
        std::string command;
        nlohmann::json argumentList;
        nlohmann::json environmentMap;
        long timeout;
        std::string execKey;
        bool isClosed = false;
    };
}