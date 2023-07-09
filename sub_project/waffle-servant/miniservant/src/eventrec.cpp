#include <iostream>
#include <fstream>
#include <mutex>
#include "eventrec.hpp"

namespace miniservant
{
    std::mutex eventrecMutex;

    const char EVENT_LABEL[] = "<WAFFLE_RESULT:";
    const char STATE_WAITING = 0;
    const char STATE_READING_NAME = 1;
    const char STATE_READING_VALUE = 2;
    const char ESCAPING_MARK = '\\';
    const char END_MARK = '>';
    const char SECTION_SEPARATING_MARK = ':';
    const char EVENT_SEPARATOR = 0x1e;
    const char EVENT_VALUE_SEPARATOR = 0x1f;

    eventrec::eventrec(std::filesystem::path* baseDirectory, std::filesystem::path* recordPath)
    {
        if (recordPath->is_absolute())
            this->recordPath = new std::filesystem::path(recordPath->string());
        else
            this->recordPath = new std::filesystem::path((*baseDirectory / *recordPath).string());
        this->state = 0;
        this->nameBuilder = "";
        this->valueBuilder = "";
        this->cursor = 0;
        this->escape = false;
    };

    void eventrec::write(std::string name, std::string value)
    {
        eventrecMutex.lock();
        auto writer = std::ofstream(*this->recordPath, std::ios::app);
        writer << name << EVENT_VALUE_SEPARATOR << value << EVENT_SEPARATOR;
        writer.close();
        eventrecMutex.unlock();
    };

    void eventrec::input(char ch)
    {
        if (state == STATE_WAITING)
        {
            if (ch == EVENT_LABEL[cursor])
            {
                cursor += 1;
                if (sizeof(EVENT_LABEL) - 1 == cursor)
                {
                    cursor = 0;
                    state = STATE_READING_NAME;
                }
            }
            else if (ch == EVENT_LABEL[0])
                cursor = 1;
            else
                cursor = 0;
        }
        else if (state == STATE_READING_NAME)
        {
            if (!escape && ch == SECTION_SEPARATING_MARK)
                state = STATE_READING_VALUE;
            else if (!escape && ch == ESCAPING_MARK)
                escape = true;
            else
            {
                escape = false;
                nameBuilder += ch;
            }
        }
        else if (state == STATE_READING_VALUE)
        {
            if (!escape && ch == END_MARK)
            {
                write(nameBuilder, valueBuilder);
                nameBuilder.clear();
                valueBuilder.clear();
                state = STATE_WAITING;
            }
            else if (!escape && ch == ESCAPING_MARK)
                escape = true;
            else
            {
                escape = false;
                valueBuilder += ch;
            }
        }
    };
}