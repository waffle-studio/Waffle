#include <vector>
#include <filesystem>

namespace miniservant
{
    struct DirHashDefaultTargets
    {
        /* data */
    };
    
    class dirhash
    {
    public:
        dirhash(std::filesystem::path, std::filesystem::path, bool);
        //~dirhash();
        static std::filesystem::path *dirhash::_init_default_target();
    private:
        static const std::filesystem::path *DEFAULT_TERGET;
    };
}