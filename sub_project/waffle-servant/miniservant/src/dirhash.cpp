#include <cstring>
#include <thread>
#include <chrono>
#include <fstream>
#include "sha256.h"
#include "dirhash.hpp"
#include <iostream>

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

    const std::vector<std::filesystem::path> dirhash::DEFAULT_TERGET = _init_default_target();
    const std::string dirhash::HASH_FILE = ".WAFFLE_HASH";
    const std::string dirhash::IGNORE_FLAG = ".WAFFLE_HASH_IGNORE";
    const std::string dirhash::SEPARATOR = ":";

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path, bool is_ready)
    {
        this->hash = nullptr;

        this->baseDirectory = base_directory;
        if (directory_path.is_absolute())
            this->directoryPath = directory_path;
        else
            this->directoryPath = base_directory / directory_path;
        this->directoryPath = _path_normalize(this->directoryPath);

        if (is_ready)
            calculate();
    };

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path)
        : dirhash::dirhash(base_directory, directory_path, true){};

    dirhash::~dirhash()
    {
        if (this->hashSize > 0)
            free(this->hash);
    }

    unsigned char* dirhash::getHash()
    {
        if (this->hashSize <= 0)
            calculate();
        return this->hash;
    }

    void dirhash::calculate()
    {
        auto fileSet = std::set<std::string>();
        auto targetList = std::vector<std::filesystem::path>();
        for (auto target : dirhash::DEFAULT_TERGET)
        {
            targetList.push_back(this->directoryPath / target);
        }
        collectFilesStatusTo(&fileSet, targetList);
        std::string chainedStatus = "";
        auto a = std::vector<std::string>();
        for (auto s : fileSet)
        {
            chainedStatus.append(s);
            chainedStatus.append(SEPARATOR);
            std::cout << s << std::endl;
            a.push_back(s);
        }
        std::cout << "----" << std::endl;
        std::random_shuffle(a.begin(), a.end());
        for (auto s : a)
        {
            std::cout << s << std::endl;
        }
        std::cout << "----" << std::endl;
        std::random_shuffle(a.begin(), a.end());
        std::sort(a.begin(), a.end());
        for (auto s : a)
        {
            std::cout << s << std::endl;
        }
        auto sha256 = ::SHA256();
        sha256.add(chainedStatus.c_str(), chainedStatus.size());
        if (this->hashSize <= 0)
            this->hash = (unsigned char*)malloc(sizeof(unsigned char) * sha256.HashBytes);
        this->hashSize = sha256.HashBytes;
        sha256.getHash(this->hash);
    };

    void dirhash::collectFileStatusTo(std::set<std::string>* fileSet, std::filesystem::path target)
    {
        if (std::filesystem::exists(target))
        {
            if (std::filesystem::is_symlink(target))
                void();
            else if (std::filesystem::is_directory(target))
                collectDirectoryStatusTo(fileSet, target);
            else if (target.filename().string() != HASH_FILE && !target.filename().string().ends_with(IGNORE_FLAG))
                (*fileSet).insert(_path_normalize(std::filesystem::relative(target, this->baseDirectory)).string() + SEPARATOR + std::to_string(std::filesystem::file_size(target)));
        }
    };

    void dirhash::collectFilesStatusTo(std::set<std::string>* fileSet, std::vector<std::filesystem::path> targets)
    {
        for (const auto target : targets)
        {
            collectFileStatusTo(fileSet, target);
        }
    };

    void dirhash::collectDirectoryStatusTo(std::set<std::string>* fileSet, std::filesystem::path target)
    {
        bool hasIgnoreFlag = false;
        for (const auto &entry : std::filesystem::directory_iterator(target))
        {
            if (entry.path().filename().string() == IGNORE_FLAG)
                return;
        }

        for (const auto &entry : std::filesystem::directory_iterator(target))
        {
            collectFileStatusTo(fileSet, entry.path());
        }
    };

    std::filesystem::path dirhash::getHashFilePath()
    {
        return this->directoryPath / HASH_FILE;
    };

    bool dirhash::hasHashFile()
    {
        return std::filesystem::exists(getHashFilePath());
    };

    bool dirhash::isMatchToHashFile()
    {
        return 0 == std::memcmp(hash, _read_hash_file(getHashFilePath()), this->hashSize);
    };

    bool dirhash::waitToMatch(int timeout)
    {
        bool isMatched = false;
        int count = 0;

        while (!(isMatched = isMatchToHashFile()) && count < timeout)
        {
            std::this_thread::sleep_for(std::chrono::seconds(1));
            calculate();
            if (timeout >= 0)
                count += 1;
        }
        return isMatched;
    };

    void dirhash::createEmptyHashFile()
    {
        if (!hasHashFile())
        {
            auto path = getHashFilePath();
            auto stream = std::ofstream(path);
            stream << "";
            stream.close();
            std::filesystem::permissions(path, std::filesystem::perms::all);
        }
    };

    void dirhash::save()
    {
        auto path = getHashFilePath();
        auto stream = std::ofstream(path, std::ios::binary);
        stream.write(reinterpret_cast<char *>(getHash()), hashSize);
        stream.close();
        std::filesystem::permissions(path, std::filesystem::perms::all);
    };

    bool dirhash::update()
    {
        if (hasHashFile())
        {
            calculate();
            if (!isMatchToHashFile())
            {
                save();
                return true;
            }
        }
        else
        {
            save();
            return true;
        }
        return false;
    };
}