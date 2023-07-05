cmake_minimum_required(VERSION 3.13)
project(miniservant CXX)
set(CMAKE_CXX_STANDARD 20)  
set(CMAKE_CXX_FLAGS "-std=c++2a -lstdc++fs")

file(GLOB main_cpp "*.cpp")
add_executable(miniservant ${main_cpp})
