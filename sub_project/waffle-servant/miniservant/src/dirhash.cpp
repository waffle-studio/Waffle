#include <cstring>
#include "sha256.h"
#include "dirhash.hpp"

namespace miniservant
{
    template <class T>
    T *_to_array(std::vector<T> vec)
    {
        T arr[vec.size()];
        std::copy(vec.begin(), vec.end(), arr);
        return arr;
    };

    inline std::vector<std::filesystem::path> _init_default_target()
    {
        static auto res = std::vector<std::filesystem::path>(4);
        res.push_back(std::filesystem::path("BASE"));
        res.push_back(std::filesystem::path("EVENT_STATUS.log"));
        res.push_back(std::filesystem::path("STDOUT.txt"));
        res.push_back(std::filesystem::path("STDERR.txt"));
        return res;
    };

    inline std::filesystem::path _path_normalize(std::filesystem::path path)
    {
        path.make_preferred();
        return std::filesystem::path(path.lexically_normal());
    };

    inline std::byte* _read_hash_file(std::filesystem::path path)
    {
        return nullptr;
    };

    std::vector<std::filesystem::path> DEFAULT_TERGET = _init_default_target();
    std::string HASH_FILE = ".WAFFLE_HASH";
    std::string IGNORE_FLAG = ".WAFFLE_HASH_IGNORE";
    std::string SEPARATOR = ":";

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path, bool is_ready)
    {
        this->hash = nullptr;

        this->baseDirectory = base_directory;
        if (!directory_path.is_absolute())
        {
            this->directoryPath = base_directory / directory_path;
        }
        this->directoryPath = _path_normalize(this->directoryPath);

        if (is_ready)
        {
            calculate();
        }
    };

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path)
        : dirhash::dirhash(base_directory, directory_path, true){};

    std::byte *dirhash::getHash()
    {
        if (this->hash == nullptr)
        {
            calculate();
        }
        return this->hash;
    }

    void dirhash::calculate()
    {
        auto fileSet = std::set<std::string>();
        auto targetList = std::vector<std::filesystem::path>();
        for (std::filesystem::path target : dirhash::DEFAULT_TERGET)
        {
            targetList.push_back(this->directoryPath / target);
        }
        collectFilesStatusTo(fileSet, targetList);
        std::string chainedStatus = "";
        for (std::string s : fileSet)
        {
            chainedStatus.append(s);
            chainedStatus.append(SEPARATOR);
        }
        SHA256 sha256;
        std::string myHash = sha256(chainedStatus);
        this->hashSize = sha256.HashBytes;
        unsigned char rawHash[sha256.HashBytes];
        sha256.getHash(rawHash);
        this->hash = (std::byte *)rawHash;
    };

    void dirhash::collectFileStatusTo(std::set<std::string> fileSet, std::filesystem::path target)
    {
        if (std::filesystem::exists(target))
        {
            if (std::filesystem::is_symlink(target))
            {
                // NOP
            }
            else if (std::filesystem::is_directory(target))
            {
                collectDirectoryStatusTo(fileSet, target);
            }
            else if (target.filename().string() != HASH_FILE && !target.filename().string().ends_with(IGNORE_FLAG))
            {
                fileSet.insert(_path_normalize(std::filesystem::relative(target, this->baseDirectory)).string() + SEPARATOR + std::to_string(std::filesystem::file_size(p)));
            }
        }
    };

    void dirhash::collectFilesStatusTo(std::set<std::string> fileSet, std::vector<std::filesystem::path> targets)
    {
        for (std::filesystem::path target : targets)
        {
            collectFileStatusTo(fileSet, target);
        }
    };

    void dirhash::collectDirectoryStatusTo(std::set<std::string> fileSet, std::filesystem::path target)
    {
        bool hasIgnoreFlag = false;
        for (const auto &entry : std::filesystem::directory_iterator(target))
        {
            if (entry.path().filename().string() == IGNORE_FLAG) {
                return;
            }
        }

        for (const auto &entry : std::filesystem::directory_iterator(target))
        {
            collectFileStatusTo(fileSet, entry.path());
        }
    };

    std::filesystem::path dirhash::getHashFilePath()
    {
        return directoryPath / HASH_FILE;
    };

    bool dirhash::hasHashFile()
    {
        return std::filesystem::exists(getHashFilePath());
    };

    bool dirhash::isMatchToHashFile()
    {
        return 0 == std::memcmp(hash, _read_hash_file(getHashFilePath()), this->hashSize);
    };

    bool dirhash::waitToMatch(int)
    {

    };

    void dirhash::createEmptyHashFile()
    {

    };

    void dirhash::save()
    {

    };

    bool dirhash::update()
    {

    };
}