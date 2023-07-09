#include <filesystem>
#include <string>

namespace miniservant
{
    class eventrec
    {
    public:
        eventrec(std::filesystem::path, std::filesystem::path);
        ~eventrec();
        void write(std::string, std::string);
        void input(char);

    private:
        std::filesystem::path* recordPath;
        char state;
        std::string nameBuilder;
        std::string valueBuilder;
        int cursor;
        bool escape;
    };
}