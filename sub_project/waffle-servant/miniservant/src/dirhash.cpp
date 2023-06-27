#include "dirhash.hpp"

namespace miniservant
{
    std::filesystem::path* dirhash::_init_default_target()
    {
        static std::filesystem::path res[4];
        return res;
    };

    std::filesystem::path* DEFAULT_TERGET = dirhash::_init_default_target();

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path, bool is_ready)
    {
    };

    bool waitForMatch(int timeout)
    {
    };
}