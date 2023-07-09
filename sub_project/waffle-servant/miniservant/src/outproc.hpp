#include <thread>
#include <filesystem>
#include "subprocess.hpp"
#include "eventrec.hpp"

namespace miniservant
{
    class outproc
    {
    public:
        outproc(subprocess::PipeHandle*, std::filesystem::path, std::filesystem::path, std::filesystem::path);
        ~outproc();
        void start();
        void join();

    private:
        std::thread* thread = nullptr;
        subprocess::PipeHandle* pipe;
        std::filesystem::path filePath;
        eventrec* recorder;
    };
}