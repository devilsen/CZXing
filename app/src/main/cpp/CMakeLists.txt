cmake_minimum_required(VERSION 3.4.1)

include_directories(include)

add_library(
        zxingc-lib
        SHARED
        native-lib.cpp JNIUtils.cpp)

add_library( zxing
        SHARED
        IMPORTED )

find_library(
        log-lib
        log)

target_link_libraries(
        zxingc-lib
        ${log-lib})