#include <filesystem>
#include "subprocess.hpp"

namespace miniservant
{
    class outproc
    {
    public:
        outproc(subprocess::PipeHandle, std::filesystem::path, std::filesystem::path, std::filesystem::path);
        void start();
        void join();
    };
}