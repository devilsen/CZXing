//
// Created by dongSen on 2023/3/31.
//

#include <jni.h>

#include "JniHelper.h"

CZXING_BEGIN_NAMESPACE()

extern "C"
jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        CZXingJNIInitializer::registerJNI(env);
    }

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        CZXingJNIInitializer::unRegisterJNI(env);
    }
}

CZXING_END_NAMESPACE()