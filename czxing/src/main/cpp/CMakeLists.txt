cmake_minimum_required(VERSION 3.15)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WARNING_FLAGS} -std=c++11 -pthread")
#set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)

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
add_library(opencv_dnn STATIC IMPORTED)
add_library(opencv_imgcodecs STATIC IMPORTED)
add_library(opencv_imgproc STATIC IMPORTED)
add_library(opencv_wechat_qrcode STATIC IMPORTED)
#
add_library(cpufeatures STATIC IMPORTED)
add_library(ittnotify STATIC IMPORTED)
add_library(libprotobuf STATIC IMPORTED)
add_library(tbb STATIC IMPORTED)
add_library(tegra_hal STATIC IMPORTED)

## 设置库路径
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_core.a)
set_target_properties(opencv_dnn PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_dnn.a)
set_target_properties(opencv_imgcodecs PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_imgcodecs.a)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_imgproc.a)
set_target_properties(opencv_wechat_qrcode PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_wechat_qrcode.a)
#
set_target_properties(cpufeatures PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libcpufeatures.a)
set_target_properties(ittnotify PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libittnotify.a)
set_target_properties(libprotobuf PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/liblibprotobuf.a)
set_target_properties(tbb PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libtbb.a)
set_target_properties(tegra_hal PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libtegra_hal.a)

# for debug
#add_library(opencv_java4 SHARED IMPORTED)
#set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../opencv/libs/${ANDROID_ABI}/libopencv_java4.so)

target_link_libraries(
        czxing
        ZXing

        opencv_imgproc
        opencv_imgcodecs
        opencv_dnn
        opencv_wechat_qrcode
        opencv_core
        cpufeatures
        ittnotify
        libprotobuf
        tbb
        tegra_hal
        #for debug
#        opencv_java4

        jnigraphics
        android
        z
        log)