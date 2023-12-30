//
// Created by JK on 7/28/20.
//


#include <malloc.h>
#include <type_traits>
#include <linux/time.h>
#include <time.h>
#include <iosfwd>
#include "imgconversionJNI.h"
#include "fast_img_conversion.h"
#include "LOG.h"

/**
 *
 * @param env
 * @param obj
 * @param data
 * @param width
 * @param height
 * @param uvOrder 0: nv12 (UV), 1: nv21 (VU)
 */

#define RGB_BYTES_PER_PIXEL 3;

/* return current time in milliseconds */
static double
now_ms(void) {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
}

bool compareArray(unsigned char const *before, unsigned char const *after, int length) {
    for (int i = 0; i < length; i++) {
        if (before[i] != after[i]) return false;
    }
    return true;
}



/**
 * param mode 0 if yuv2rgb, 1 if reverted
 */
unsigned char *convert(int outType, unsigned char const *in, int inType,
                       int width, int height, unsigned char * uv = nullptr) {
    double t0, t1, time_c, time_neon;
    int frameSize = width * height;
    int outFrameByteLen = frameSize * 3 / 2; //YUV
    if (outType == RGB_TYPE::RGB || outType == RGB_TYPE::BGR) {
        outFrameByteLen = frameSize * 3;
    }

    auto *out = static_cast<unsigned char *>(malloc(outFrameByteLen * sizeof(unsigned char)));

    t0 = now_ms();
    switch (inType) {

        case YUV_TYPE::NV21:

            if (outType == RGB_TYPE::RGB) {
                yuv_to_rgb(CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID,out, RGB_TYPE::RGB, in,
                        YUV_TYPE::NV21, width, height,
                        uv);
            } else {
                yuv_to_rgb(CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID,out, RGB_TYPE::BGR, in, YUV_TYPE::NV21, width, height);
            }

            break;

        case YUV_TYPE::NV12:
            if (outType == RGB_TYPE::RGB) {
                yuv_to_rgb(CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID,out, RGB_TYPE::RGB, in, YUV_TYPE::NV12, width,
                        height, uv);
            } else {
                yuv_to_rgb(CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID,out, RGB_TYPE::BGR, in, YUV_TYPE::NV12, width, height);
            }

            break;

        case RGB_TYPE::RGB:
            if (outType == YUV_TYPE::NV21) {
                rgb_to_yuv(CONVERSION_TYPE::RGB_TO_YUV_INT_FULL_SWING, out, YUV_TYPE::NV21, in,
                        RGB_TYPE::RGB, width,
                        height);
            } else {
                rgb_to_yuv(CONVERSION_TYPE::RGB_TO_YUV_INT_FULL_SWING, out, YUV_TYPE::NV12, in, RGB_TYPE::RGB, width,
                        height);
            }

            break;

        case RGB_TYPE::BGR:

            if (outType == YUV_TYPE::NV12) {
                rgb_to_yuv(CONVERSION_TYPE::RGB_TO_YUV_INT_FULL_SWING, out, YUV_TYPE::NV12, in,
                        RGB_TYPE::BGR, width, height);
            } else {
                rgb_to_yuv(CONVERSION_TYPE::RGB_TO_YUV_INT_FULL_SWING, out, YUV_TYPE::NV21, in, RGB_TYPE::BGR, width, height);
            }
            break;
        default:
            return nullptr;

    }
    t1 = now_ms();
    time_neon = t1 - t0;
    if (inType == YUV_TYPE::NV12 || inType == YUV_TYPE::NV21) {
        LOGI("NEON convert %s size (%d,%d) take %g (ms)", "yuv2rgb", width, height,
                time_neon);
    }
    else {
        LOGI( "NEON convert %s size (%d,%d) take %g (ms)", "rgb2yuv", width, height, time_neon);

    }

    return out;
}


JCMCPRV(jbyteArray, convertYuvToRgb)
(JNIEnv * env, jclass obj, jint rgbOutType, jbyteArray data, jint yuvInType, jint width, jint
height) {

    //parse Java input
    jbyte *bufferPtr = env->GetByteArrayElements(data, nullptr);
    auto yuv = reinterpret_cast<unsigned char *>(bufferPtr);

    //do convert
    unsigned char *rgb_out = convert(rgbOutType, yuv, yuvInType, width, height);

    //return converted data
    int frameByteLen = width * height * 3;
    jbyteArray ret = env->NewByteArray(frameByteLen);
    env->SetByteArrayRegion(ret, 0, frameByteLen, reinterpret_cast<signed char *>(rgb_out));

    //release
    env->ReleaseByteArrayElements(data, bufferPtr, 0);

    if (rgb_out != nullptr) free(rgb_out);

    return ret;
}

//// from YUV, separated Y and YU
//JCMCPRV(jbyteArray, convertYuvToRgb)
//(JNIEnv * env, jclass obj, jint out_RgbType, jbyteArray in_yData, jbyteArray in_uvData, jint
//yuvInType, jint width, jint height){
//    //parse Java input
//    jbyte *ybufferPtr = env->GetByteArrayElements(in_yData, nullptr);
//    auto y = reinterpret_cast<unsigned char *>(ybufferPtr);
//
//    jbyte *yubufferPtr = env->GetByteArrayElements(in_uvData, nullptr);
//    auto uv = reinterpret_cast<unsigned char *>(yubufferPtr);
//    //do convert
//    unsigned char *rgb_out = convert(out_RgbType, y, yuvInType, width, height, uv);
//
//    //return converted data
//    int frameByteLen = width * height * 3;
//    jbyteArray ret = env->NewByteArray(frameByteLen);
//    env->SetByteArrayRegion(ret, 0, frameByteLen, reinterpret_cast<signed char *>(rgb_out));
//
//    //release
//    env->ReleaseByteArrayElements(in_yData, ybufferPtr, 0);
//
//    if (rgb_out != nullptr) free(rgb_out);
//
//    return ret;
//}

JCMCPRV(jbyteArray, convertRgbToYuv)
(JNIEnv * env, jclass obj, jint yuvOutType, jbyteArray data, jint rgbInType, jint width, jint
height) {
    //parse Java input
    jbyte *bufferPtr = env->GetByteArrayElements(data, nullptr);
    auto rgb = reinterpret_cast<unsigned char *>(bufferPtr);

    //do convert
    unsigned char *yuv_out = convert(yuvOutType, rgb, rgbInType, width, height);

    //return converted data
    int frameByteLen = width * height * 3 / 2;
    jbyteArray ret = env->NewByteArray(frameByteLen);
    env->SetByteArrayRegion(ret, 0, frameByteLen, reinterpret_cast<signed char *>(yuv_out));

    // release
    env->ReleaseByteArrayElements(data, bufferPtr, 0);

    if (yuv_out != nullptr) free(yuv_out);
    return ret;
}

JCMCPRV(jbyteArray, doTestConversion)
(JNIEnv *env, jclass obj, jbyteArray yuvData, jint width, jint height) {


    jbyte *bufferPtr = env->GetByteArrayElements(yuvData, nullptr);
    auto yuv = reinterpret_cast<unsigned char *>(bufferPtr);

    unsigned char *rgb_out = convert(RGB_TYPE::RGB, yuv, YUV_TYPE::NV21, width, height);
    unsigned char *yuv_out =  convert(YUV_TYPE::NV21, rgb_out, RGB_TYPE::RGB, width, height);

    int frameByteLen = width * height * 3 / 2;

    jbyteArray ret = env->NewByteArray(frameByteLen);
    env->SetByteArrayRegion(ret, 0, frameByteLen, reinterpret_cast<signed char *>(yuv_out));
    env->ReleaseByteArrayElements(yuvData, bufferPtr, 0);

    if (rgb_out != nullptr) free(rgb_out);
    if (yuv_out != nullptr) free(yuv_out);

    return ret;
}

JCMCPRV(jfloat , getLibVersion)
(JNIEnv *env, jclass obj) {
   // auto version  = (jstring) (const char *) (env)->NewStringUTF(getVersion());
    LOGI("neon_lib_version: %.1f ", getVersion());
    return getVersion();
}
