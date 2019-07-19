cmake_minimum_required(VERSION 3.4.1)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WARNING_FLAGS} -std=c++11 -pthread ")

add_library(
        zxing-lib
        SHARED
        native-lib.cpp JNIUtils.cpp QRCodeRecognizer.cpp ImageUtil.cpp)

add_subdirectory(zxing)
include_directories(zxing)

include_directories(include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")

find_library(
        log-lib
        log)

target_link_libraries(
        zxing-lib
        jnigraphics
        ZXingCore
        opencv_java4
        android
        ${log-lib})