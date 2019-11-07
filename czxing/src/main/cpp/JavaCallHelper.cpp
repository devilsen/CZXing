//
// Created by dongSen on 2019/08/10 0010.
//

#include "JavaCallHelper.h"
#include "JNIUtils.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) : javaVM(_javaVM),
                                                                                env(_env) {
    jSdkObject = env->NewGlobalRef(_jobj);

    jclass jSdkClass = env->GetObjectClass(jSdkObject);
    if (jSdkClass == nullptr) {
        LOGE("Unable to find class");
        return;
    }

    jmid_on_result = env->GetMethodID(jSdkClass, "onDecodeCallback", "(Ljava/lang/String;I[F)V");
    jmid_on_focus = env->GetMethodID(jSdkClass, "onFocusCallback", "()V");
    jmid_on_brightness = env->GetMethodID(jSdkClass, "onBrightnessCallback", "(Z)V");

    if (jmid_on_result == nullptr) {
        LOGE("jmid_on_result is null");
    }
}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(jSdkObject);
//    DELETE(javaVM);
//    DELETE(env);
}

void JavaCallHelper::onResult(const ZXing::Result &result) {
    if (!result.isValid() && !result.isBlurry()) {
        return;
    }

    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    int mNeedDetach = JNI_FALSE;
    if (getEnvStat == JNI_EDETACHED) {
        //如果没有， 主动附加到jvm环境中，获取到env
        if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        mNeedDetach = JNI_TRUE;
    }

    std::string contentWString;
    jstring mJstring = nullptr;
    jint format = -1;
    jfloatArray pointsArray = env->NewFloatArray(0);

    if (result.isValid()) {
        contentWString = UnicodeToANSI(result.text());
        mJstring = env->NewStringUTF(contentWString.c_str());
        format = static_cast<int>(result.format());
    }

    if (result.isBlurry()) {
        std::vector<ZXing::ResultPoint> resultPoints = result.resultPoints();
        int size = static_cast<int>(result.resultPoints().size() * 2);
        pointsArray = env->NewFloatArray(size);

        jfloat points[size];

        int index = 0;
        for (auto point : resultPoints) {
            points[index++] = point.x();
            points[index++] = point.y();
        }

        env->SetFloatArrayRegion(pointsArray, 0, size, points);
    }

    env->CallVoidMethod(jSdkObject, jmid_on_result, mJstring, format, pointsArray);

    //释放当前线程
    if (mNeedDetach) {
        javaVM->DetachCurrentThread();
    }

}

void JavaCallHelper::onFocus() {
    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    int mNeedDetach = JNI_FALSE;
    if (getEnvStat == JNI_EDETACHED) {
        //如果没有， 主动附加到jvm环境中，获取到env
        if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        mNeedDetach = JNI_TRUE;
    }

    env->CallVoidMethod(jSdkObject, jmid_on_focus);

    //释放当前线程
    if (mNeedDetach) {
        javaVM->DetachCurrentThread();
    }
}

void JavaCallHelper::onBrightness(const bool isDark) {
    int getEnvStat = javaVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    int mNeedDetach = JNI_FALSE;
    if (getEnvStat == JNI_EDETACHED) {
        //如果没有， 主动附加到jvm环境中，获取到env
        if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        mNeedDetach = JNI_TRUE;
    }

    env->CallVoidMethod(jSdkObject, jmid_on_brightness, isDark);

    //释放当前线程
    if (mNeedDetach) {
        javaVM->DetachCurrentThread();
    }

}
