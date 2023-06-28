#include "dirhash.hpp"

namespace miniservant
{
    std::filesystem::path* dirhash::_init_default_target()
    {
        static std::filesystem::path res[4];
        res[0] = std::filesystem::path("BASE");
        res[1] = std::filesystem::path("EVENT_STATUS.log");
        res[2] = std::filesystem::path("STDOUT.txt");
        res[3] = std::filesystem::path("STDERR.txt");
        return res;
    };

    std::filesystem::path dirhash::_path_normalize(std::filesystem::path path)
    {
        path.make_preferred();
        return std::filesystem::path(path.lexically_normal());
    };

    std::filesystem::path* DEFAULT_TERGET = dirhash::_init_default_target();
    std::string HASH_FILE = ".WAFFLE_HASH";
    std::string IGNORE_FLAG = ".WAFFLE_HASH_IGNORE";
    std::string SEPARATOR = ":";

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path, bool is_ready)
    {
        this->base_directory = base_directory;
        if (! directory_path.is_absolute()) {
            this->directory_path = base_directory / directory_path;
        }
        this->directory_path = _path_normalize(this->directory_path);

        if (is_ready) {
            calculate();
        }
    };

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path)
        : dirhash::dirhash(base_directory, directory_path, true)
    {
    };

    void dirhash::calculate() {

    };

    bool waitForMatch(int timeout)
    {
        return false;
    };
}