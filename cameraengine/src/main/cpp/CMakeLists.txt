cmake_minimum_required(VERSION 3.4.1)

project(img_converion_neon)
# build cpufeatures as a static lib
#add_library(cpufeatures STATIC
#        ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c)

# build app's shared lib

# set up neon build flag for file using intrinsics
# name: helloneon-intrinsics.c (It is named EXACTLY as this on disk,
#                              just like a normal source file)
# then set up neon flag for neon files
# [This example only build for armeabi-v7a, x86 could be done the same way]
#

if (${LIBTYPE} STREQUAL "static")
    set(STATIC_LIB TRUE)
endif ()

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    # make a list of neon files and add neon compiling flags to them
    set(neon_SRCS yuv2rgb.cpp rgb2yuv.cpp)
    set_property(SOURCE ${neon_SRCS}
            APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
    add_definitions("-DARM_NEON=1")
    #    add_definitions("-DCMAKE_BUILD_TYPE=Release")

    find_library( # Sets the name of the path variable.
            log-lib
            log)

    if (STATIC_LIB)
        add_library(img-conversion STATIC imgconversionJNI.cpp fast_img_conversion.cpp ${neon_SRCS})
    else ()
        add_library(img-conversion SHARED imgconversionJNI.cpp fast_img_conversion.cpp ${neon_SRCS})
    endif ()

    target_link_libraries(img-conversion ${log-lib})


elseif (${ANDROID_ABI} STREQUAL "x86")
    #    set(neon_SRCS yuv2rgb.cpp)
    set(CMAKE_CXX_FLAGS "-std=c++14 -stdlib=libc++")
    set(neon_SRCS yuv2rgb.cpp rgb2yuv.cpp)
    set_property(SOURCE ${neon_SRCS} APPEND_STRING PROPERTY COMPILE_FLAGS
            " -mssse3  -Wno-unknown-attributes \
                   -Wno-deprecated-declarations \
                   -Wno-constant-conversion \
                   -Wno-static-in-inline")
    add_definitions(-DX86_NEON=1)
    add_library(img-conversion STATIC fast_img_conversion.cpp ${neon_SRCS})
    target_link_libraries(img-conversion)
    #    add_definitions("-DCMAKE_BUILD_TYPE=Release")

else ()
    set(neon_SRCS)
endif ()


#
#[[### build by cmake
cmake \
-DCMAKE_BUILD_TYPE=Release \
-DCMAKE_TOOLCHAIN_FILE=/Users/jk/Library/Android/sdk/ndk/18.1.5063045/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_NATIVE_API_LEVEL=28 ..
##]]

