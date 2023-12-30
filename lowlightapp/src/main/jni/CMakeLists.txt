cmake_minimum_required(VERSION 3.4.1)

find_library(
        log-lib
        log)

include_directories(${CMAKE_SOURCE_DIR}/../../../includes)

add_library(pytorch-lib SHARED IMPORTED)
set_target_properties(pytorch-lib PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libpytorch_jni.so)
add_library(libfb SHARED IMPORTED)
set_target_properties(libfb PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libfbjni.so)
add_library(libc++_shared SHARED IMPORTED)
set_target_properties(libc++_shared PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libc++_shared.so)


add_library(lib_opencv_1 SHARED IMPORTED)
set_target_properties(lib_opencv_1 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libopencv_core.so)

add_library(lib_opencv_2 SHARED IMPORTED)
set_target_properties(lib_opencv_2 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libopencv_highgui.so)

add_library(lib_opencv_3 SHARED IMPORTED)
set_target_properties(lib_opencv_3 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libopencv_imgcodecs.so)

add_library(lib_opencv_4 SHARED IMPORTED)
set_target_properties(lib_opencv_4 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libopencv_imgproc.so)

add_library(lib_opencv_5 SHARED IMPORTED)
set_target_properties(lib_opencv_5 PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../../../libs/libopencv_videoio.so)

add_library(lib_opencv INTERFACE)
target_link_libraries(lib_opencv INTERFACE lib_opencv_1 lib_opencv_2 lib_opencv_3 lib_opencv_4 lib_opencv_5)

add_library(lowlight SHARED jni_interface.cpp lowlight.cpp)

target_link_libraries(
    lowlight
    pytorch-lib
    libfb
    libc++_shared
    lib_opencv
    android
    ${log-lib}
    log
)
