//
// Created by dongSen on 2023/3/31
//

#include <jni.h>
#include "ScanResult.h"
#include "config.h"

USING_CZXING_NAMESPACE()

static struct CodeResult {
    jclass clazz;
    jmethodID builder;
} j_codeResult;

void ScanResult::init(JNIEnv* env)
{
    jclass resultClass = env->FindClass("me/devilsen/czxing/code/CodeResult");
    j_codeResult = {
            reinterpret_cast<jclass>(env->NewGlobalRef(resultClass)),
            env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;I[II)V"),
    };
    env->DeleteLocalRef(resultClass);
}

void ScanResult::unInit(JNIEnv* env)
{
    env->DeleteGlobalRef(j_codeResult.clazz);
}

jobjectArray ScanResult::obtainResultArray(JNIEnv* env, int size)
{
    return env->NewObjectArray(size, j_codeResult.clazz, nullptr);
}

jintArray getJavaArray(JNIEnv *env, const czxing::CodeRect& codeRect)
{
    jintArray array = env->NewIntArray(4);
    env->SetIntArrayRegion(array, 0, 1, &codeRect.x);
    env->SetIntArrayRegion(array, 1, 1, &codeRect.y);
    env->SetIntArrayRegion(array, 2, 1, &codeRect.width);
    env->SetIntArrayRegion(array, 3, 1, &codeRect.height);

    return array;
}

jobject ScanResult::getJCodeResult(JNIEnv* env)
{
    auto text = env->NewStringUTF(m_text.c_str());
    auto format = static_cast<int>(m_codeFormat);
    return env->NewObject(j_codeResult.clazz, j_codeResult.builder, text, format, getJavaArray(env, rect()), 0);
}

