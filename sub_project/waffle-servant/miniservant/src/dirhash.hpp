#include <string>
#include <vector>
#include <filesystem>

namespace miniservant
{
    class dirhash
    {
    public:
        static inline std::filesystem::path* _init_default_target();
        static inline std::filesystem::path _path_normalize(std::filesystem::path);
        static const std::string HASH_FILE;
        static const std::string IGNORE_FLAG;

        dirhash(std::filesystem::path, std::filesystem::path, bool);
        dirhash(std::filesystem::path, std::filesystem::path);
        //~dirhash();
        void calculate();
    private:
        static const std::filesystem::path *DEFAULT_TERGET;
        static const std::string SEPARATOR;

        std::filesystem::path base_directory;
        std::filesystem::path directory_path;
        std::byte* hash;
    };
}