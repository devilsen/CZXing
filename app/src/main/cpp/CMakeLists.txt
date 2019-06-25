cmake_minimum_required(VERSION 3.4.1)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WARNING_FLAGS} -std=c++11 -pthread ")

add_library(
        zxing-lib
        SHARED
        native-lib.cpp JNIUtils.cpp yuv2bmp.c)

add_subdirectory(zxing)
include_directories(zxing)

find_library(
        log-lib
        log)

target_link_libraries(
        zxing-lib
        jnigraphics
        ZXingCore
        ${log-lib})