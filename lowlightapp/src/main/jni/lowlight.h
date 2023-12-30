//
// Created by ubuntu on 21/09/2020.
//

#ifndef LOWLIGHTAPP_LOWLIGHT_H
#define LOWLIGHTAPP_LOWLIGHT_H

#include <jni.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <torch/script.h>

#include "tensorflow/lite/model.h"
#include "tensorflow/lite/kernels/register.h"


#if defined(__ANDROID__)
#include "tensorflow/lite/delegates/gpu/gl_delegate.h"
#include "tensorflow/lite/delegates/gpu/delegate.h"
#endif

class LowLightSDK {
public:
    LowLightSDK(const char* hdrnet_path);
    LowLightSDK(AAssetManager* mgr, const char* hdrnet_path);
    ~LowLightSDK();
    cv::Mat preprocessImage(cv::Mat image);
    cv::Mat postprocessImage(cv::Mat image);
    jbyteArray enhance(JNIEnv *env, jbyteArray input, jint width, jint height);
private:
    int tensorImageWidth = 750;
    int tensorImageHeight = 1000;
    int inputImageWidth = 3000;
    int inputImageHeight = 4000;
    int outputImageWidth = 1500;
    int outputImageHeight = 2000;
    bool _moved_to_gpu = false;
    bool rotate = false;
    torch::jit::Module hdrnet_model;
public:
    struct timeval start_time, stop_time;
    int elapsedMs;
    static LowLightSDK* instance;
};

#endif //LOWLIGHTAPP_LOWLIGHT_H

