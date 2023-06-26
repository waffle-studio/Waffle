#include <vector>
#include <filesystem>

namespace miniservant {
    class dirhash {
    public:
        dirhash(std::filesystem::path, std::filesystem::path, bool);
        static std::filesystem::path* _init_default_target();
    private:
        static const std::filesystem::path* DEFAULT_TERGET;
    };
}