#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include "lowlight.h"

#define TAG "LowLightSDK"
#define PRINT_D(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define PRINT_E(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C"
void setElapsedTime(JNIEnv *env, jobject thiz) {
    jclass thisClass = env->GetObjectClass(thiz);
    jfieldID fidElapsedMs = env->GetFieldID(thisClass, "elapsedMs", "I");
    if (fidElapsedMs == NULL) {
        PRINT_D("fidElapsedMs is null");
        return;
    }
    jint elapsedMs = LowLightSDK::instance->elapsedMs;
    env->SetIntField(thiz, fidElapsedMs, elapsedMs);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    PRINT_D("JNI_OnLoad");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    PRINT_D("JNI_OnUnload");
}

extern "C" JNIEXPORT void JNICALL
Java_com_hdrcam_lowlight_LowLightSDK_init(JNIEnv *env, jobject thiz, jobject assetManager) {
    if (LowLightSDK::instance == nullptr) {
        AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
        LowLightSDK::instance = new LowLightSDK(mgr, "scripted2.pt");
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_hdrcam_lowlight_LowLightSDK_enhance(JNIEnv *env, jobject thiz, jbyteArray input, jint width, jint height) {
    if (LowLightSDK::instance == nullptr) {
        PRINT_D("Instance is null");
        return NULL;
    }
    jbyteArray output = LowLightSDK::instance->enhance(env, input, width, height);
    setElapsedTime(env, thiz);
    return output;
}
