#include "eventrec.hpp"
#include <iostream>
#include <mutex>

namespace miniservant
{
    std::mutex eventrecMutex;

    eventrec::eventrec(std::filesystem::path baseDirectory, std::filesystem::path eventFilePath)
    {
    };

    void eventrec::input(char val)
    {
        eventrecMutex.lock();
        std::cout << val;
        eventrecMutex.unlock();
    };
}