#include <thread>
#include <filesystem>
#include "eventrec.hpp"
#include "subprocess.hpp"

namespace miniservant
{
    class outproc
    {
    public:
        outproc(subprocess::PipeHandle*, std::filesystem::path, std::filesystem::path, std::filesystem::path);
        void start();
        void join();

    //private:
        std::thread* thread = nullptr;
        subprocess::PipeHandle* pipe;
        std::filesystem::path filePath;
        eventrec recorder;
    };
}