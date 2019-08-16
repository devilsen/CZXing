//
// Created by Administrator on 2019/08/10 0010.
//

#include "JavaCallHelper.h"
#include "JNIUtils.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jclass &_jobj) : javaVM(_javaVM),
                                                                               env(_env) {
    // 获取Class
    jclass jSdkClass = env->FindClass("me/devilsen/czxing/code/NativeSdk");
    if (jSdkClass == nullptr) {
        LOGE("Unable to find class");
        return;
    }

    // 获取构造函数
    jmethodID constructor = env->GetMethodID(jSdkClass, "<init>", "()V");
    if (constructor == nullptr) {
        LOGE("can't constructor jClass");
        return;
    }

    // 获取对应函数
    jSdkObject = env->NewObject(jSdkClass, constructor);
    if (jSdkObject == nullptr) {
        LOGE("can't new jobject");
        return;
    }
    jSdkObject = env->NewGlobalRef(jSdkObject);

    jmid_on_result = env->GetMethodID(jSdkClass, "onTest", "(I)V");
//    jmid_points = env->GetMethodID(jSdkClass, "onJniCallbackPoints", "([F)V");

    if (jmid_on_result == nullptr) {
        LOGE("jmid_on_result is null");
    }
}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(jSdkObject);
    DELETE(javaVM);
    DELETE(env);
}

void JavaCallHelper::onResult(ZXing::Result *result) {

}

void JavaCallHelper::onTest() {
    //获取当前native线程是否有没有被附加到jvm环境中
    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    int mNeedDetach = JNI_FALSE;
    if (getEnvStat == JNI_EDETACHED) {
        //如果没有， 主动附加到jvm环境中，获取到env
        if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        mNeedDetach = JNI_TRUE;
    }

//    jstring mJstring = env->NewStringUTF("I am from C++ Thread");
//    jlong point = 12;
//    env->CallVoidMethod(jSdkObject, jmid_on_result, mJstring, point);
    jint a = 10;
    env->CallVoidMethod(jSdkObject, jmid_on_result, a);

    //释放当前线程
    if (mNeedDetach) {
        javaVM->DetachCurrentThread();
    }
}

