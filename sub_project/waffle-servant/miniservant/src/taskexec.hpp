#include <string>
#include <vector>
#include <filesystem>

namespace miniservant
{
    class taskexec
    {
    public:

    // private:
        std::vector<std::string> environmentList;
        std::filesystem::path executableBaseDirectory;
        std::string projectName;
        std::string workspaceName;
        std::string executableName;
        std::string command;

        long timeout;
        std::string pid;
        std::string execKey;
    };
}