#include "dirhash.hpp"

namespace miniservant {
    class dirhash
    {
    private:
        static const std::filesystem::path* DEFAULT_TERGET;

    public:
        dirhash(/* args */);
        ~dirhash();
    };
    
    dirhash::dirhash(/* args */)
    {
    }
    
    dirhash::~dirhash()
    {
    }
    
    std::filesystem::path* dirhash::_init_default_target()
    {
        static std::filesystem::path res[4];
        return res;
    };

    static const std::filesystem::path* DEFAULT_TERGET = dirhash::_init_default_target();

    dirhash::dirhash(std::filesystem::path base_directory, std::filesystem::path directory_path, boolean is_ready)
    {
    };

    bool waitForMatch(int timeout)
    {
    };
}