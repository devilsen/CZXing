cmake_minimum_required(VERSION 3.4.1)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WARNING_FLAGS} -std=c++11 -pthread ")

add_library(
        czxing
        SHARED
        native-lib.cpp JNIUtils.cpp QRCodeRecognizer.cpp ImageUtil.cpp)

add_subdirectory(zxing)
include_directories(zxing)

include_directories(include)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")

# 静态方式导入库
add_library(opencv_core STATIC IMPORTED)
add_library(opencv_imgcodecs STATIC IMPORTED)
add_library(opencv_imgproc STATIC IMPORTED)

add_library(cpufeatures STATIC IMPORTED)
add_library(tbb STATIC IMPORTED)
add_library(tegra_hal STATIC IMPORTED)

# 设置库路径
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_core.a)
set_target_properties(opencv_imgcodecs PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_imgcodecs.a)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_imgproc.a)

set_target_properties(cpufeatures PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libcpufeatures.a)
set_target_properties(tbb PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libtbb.a)
set_target_properties(tegra_hal PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libtegra_hal.a)

find_library(
        log-lib
        log)

target_link_libraries(
        czxing
        ZXingCore

        opencv_imgcodecs
        opencv_imgproc
        opencv_core

        cpufeatures
        tbb
        tegra_hal

        jnigraphics
        android
        z
        ${log-lib})