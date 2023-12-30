//
// Created by JK on 7/28/20.
//

#ifndef _LOG_H_
#define _LOG_H_

#endif //V_CAMERA2GL3_LOG_H

#include <android/log.h>

#define DEBUG 1

#define  LOG_TAG "neon-img-conversion"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#if DEBUG
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...)



#endif //HELLO_NEON_LOG_H
