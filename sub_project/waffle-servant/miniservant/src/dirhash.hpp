#include <string>
#include <vector>
#include <set>
#include <filesystem>

namespace miniservant
{
    class dirhash
    {
    public:
        static const std::string HASH_FILE;
        static const std::string IGNORE_FLAG;
        static const std::vector<std::filesystem::path> DEFAULT_TERGET;
        static const std::string SEPARATOR;

        dirhash(std::filesystem::path, std::filesystem::path, bool);
        dirhash(std::filesystem::path, std::filesystem::path);
        ~dirhash();
        unsigned char* getHash();
        void calculate();
        void collectFileStatusTo(std::set<std::string>*, std::filesystem::path);
        void collectFilesStatusTo(std::set<std::string>*, std::vector<std::filesystem::path>);
        void collectDirectoryStatusTo(std::set<std::string>*, std::filesystem::path);
        std::filesystem::path getHashFilePath();
        bool hasHashFile();
        bool isMatchToHashFile();
        bool waitToMatch(int);
        void createEmptyHashFile();
        void save();
        bool update();

    private:
        std::filesystem::path* baseDirectory;
        std::filesystem::path* directoryPath;
        unsigned char* hash;
        short hashSize = 0;
    };
}