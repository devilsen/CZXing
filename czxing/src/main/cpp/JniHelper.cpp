// 
// Created by dongSen on 3/31/23.
//

#include <pthread.h>
#include "JniHelper.h"
#include "ScanResult.h"

CZXING_BEGIN_NAMESPACE()

JavaVM *g_jvm = nullptr;

static pthread_key_t g_threadKey;

void JNI_ThreadDestroyed(void *value)
{
    JNIEnv *env = (JNIEnv *) value;
    if (env != nullptr && g_jvm != nullptr) {
        g_jvm->DetachCurrentThread();
        pthread_setspecific(g_threadKey, nullptr);
    }
}

JNIEnv *getEnv()
{
    if (g_jvm == nullptr) {
        return nullptr;
    }

    JNIEnv* env;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        return env;
    }

    if (g_threadKey == 0) {
        pthread_key_create(&g_threadKey, JNI_ThreadDestroyed);
    }

    if (g_jvm->AttachCurrentThread(&env, NULL) == JNI_OK) {
        pthread_setspecific(g_threadKey, env);
        return env;
    }

    return nullptr;
}

void CZXingJNIInitializer::registerJNI(JNIEnv * env)
{
    env->GetJavaVM(&g_jvm);

    ScanResult::init(env);
}


void CZXingJNIInitializer::unRegisterJNI(JNIEnv * env)
{
    ScanResult::unInit(env);
}

CZXING_END_NAMESPACE()
