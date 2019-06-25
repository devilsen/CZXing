cmake_minimum_required(VERSION 3.4.1)

include_directories(include)

add_library( zxing
        SHARED
        IMPORTED )
set_target_properties(zxing PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}/libzxing.so)

add_library(
        zxing_android
        SHARED
        native-lib.cpp JNIUtils.cpp JNIUtils.h)

find_library(
        log-lib
        log)

target_link_libraries(
        zxing_android
        jnigraphics
        zxing
        ${log-lib})