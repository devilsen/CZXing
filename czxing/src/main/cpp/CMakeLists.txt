cmake_minimum_required(VERSION 3.4.1)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WARNING_FLAGS} -std=c++11 -pthread ")

file(GLOB SOURCE ${CMAKE_SOURCE_DIR}/*.cpp)

add_library(
        czxing
        SHARED
        ${SOURCE})

add_subdirectory(zxing)
include_directories(zxing)

include_directories(include)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")

#add_compile_options(czxing "-O1" "-fPIC")

# 静态方式导入库
add_library(opencv_core STATIC IMPORTED)
add_library(opencv_imgproc STATIC IMPORTED)

add_library(cpufeatures STATIC IMPORTED)
add_library(tbb STATIC IMPORTED)
add_library(tegra_hal STATIC IMPORTED)

## 设置库路径
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_core.a)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_imgproc.a)

set_target_properties(cpufeatures PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libcpufeatures.a)
set_target_properties(tbb PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libtbb.a)
set_target_properties(tegra_hal PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/3rdparty/libs/${ANDROID_ABI}/libtegra_hal.a)

#add_library(opencv_java4 SHARED IMPORTED)
#set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}/libopencv_java4.so)

# 导入zbar
#add_library(iconv SHARED IMPORTED)
add_library(zbar_core SHARED IMPORTED)
#set_target_properties(iconv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}/libiconv.so)
set_target_properties(zbar_core PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}/libzbarjni.so)

find_library(
        log-lib
        log)

target_link_libraries(
        czxing
        ZXingCore

#        iconv
        zbar_core

        opencv_imgproc
        opencv_core
        cpufeatures
        tbb
        tegra_hal

#        opencv_java4

        jnigraphics
        android
        z
        ${log-lib})