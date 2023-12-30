//
// Created by JK on 7/28/20.
//

#ifndef _IMGCONVERSIONJNI_H_
#define _IMGCONVERSIONJNI_H_

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JCMCPRV(rettype, name)                                             \
  rettype JNIEXPORT JNICALL Java_com_hdrcam_camera_utils_NativeConversion_##name

  // from YUV
JCMCPRV(jbyteArray, convertYuvToRgb)
(JNIEnv * env, jclass obj, jint out_RgbType, jbyteArray in_YuvData, jint yuvInType, jint width, jint
height);
//
//// from YUV, separated Y and YU
//JCMCPRV(jbyteArray, convertYuvToRgb)
//(JNIEnv * env, jclass obj, jint out_RgbType, jbyteArray in_yData, jbyteArray in_uvData, jint
//yuvInType, jint width, jint height);

// to YUV
JCMCPRV(jbyteArray, convertRgbToYuv)
(JNIEnv * env, jclass obj, jint out_YuType, jbyteArray in_RgbData, jint rgbInType, jint width, jint
height);


// util functions
JCMCPRV(jfloat, getLibVersion)
(JNIEnv * env, jclass obj);

JCMCPRV(jbyteArray, doTestConversion)
(JNIEnv * env, jclass obj, jbyteArray data, jint width, jint height);

#ifdef __cplusplus
}
#endif

#endif // IMGCONVERSIONJNI_H_