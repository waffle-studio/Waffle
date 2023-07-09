#include <fstream>
#include "outproc.hpp"

namespace miniservant
{
    void outprocThreadFunc(subprocess::PipeHandle* pipe, std::filesystem::path filePath, eventrec* recorder)
    {
        auto writer = std::ofstream(filePath);
        ssize_t len;
        char buf[1] = {0};
        while (len = subprocess::pipe_read(*pipe, buf, 1) > 0)
        {
            if (len == 1)
            {
                writer << buf[0];

                if (len == 1 && buf[0] == '\n')
                    writer.flush();

                if (recorder != nullptr)
                    recorder->input(buf[0]);
            }
        }
        writer.close();
    };

    outproc::outproc(subprocess::PipeHandle* pipe, std::filesystem::path filePath, std::filesystem::path baseDirectory, std::filesystem::path eventFilePath)
    {
        this->pipe = pipe;
        this->filePath = filePath;
        this->recorder = new eventrec(baseDirectory, eventFilePath);
    };

    outproc::~outproc()
    {
        if (this->thread != nullptr)
            delete this->thread;
        delete this->recorder;
    }

    void outproc::start()
    {
        this->thread = new std::thread(outprocThreadFunc, this->pipe, this->filePath, this->recorder); 
    };

    void outproc::join()
    {
        if (this->thread != nullptr && this->thread->joinable())
        {
            this->thread->join();
        }
    };
}