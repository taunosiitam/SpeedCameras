cmake_minimum_required(VERSION 3.22.1)
project("speedcameras")

set(CMAKE_CXX_STANDARD 17)

add_library(speedcameras SHARED speedcameras.cpp points.cpp polygons.cpp lest.cpp)
find_library(log-lib log)
target_link_libraries(speedcameras ${log-lib})
